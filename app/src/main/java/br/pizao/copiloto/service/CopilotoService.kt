package br.pizao.copiloto.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.view.TextureView
import androidx.lifecycle.LifecycleService
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.ConfirmationAction
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.network.WatsonApi
import br.pizao.copiloto.network.model.WatsonRequest
import br.pizao.copiloto.network.model.WatsonResponse
import br.pizao.copiloto.ui.overlay.GraphicOverlay
import br.pizao.copiloto.utils.Constants
import br.pizao.copiloto.utils.Constants.CAMERA_ON_BACKGROUND
import br.pizao.copiloto.utils.Constants.CAMERA_START_ACTION
import br.pizao.copiloto.utils.Constants.EXTRA_TEXT
import br.pizao.copiloto.utils.Constants.REQUEST_SPEECH_ACTION
import br.pizao.copiloto.utils.Constants.REQUEST_WATSON_ACTION
import br.pizao.copiloto.utils.Constants.SERVICE_MESSAGE_INDEX
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.Constants.TTS_ENABLED
import br.pizao.copiloto.utils.helpers.IntentHelper
import br.pizao.copiloto.utils.helpers.NotificationHelper
import br.pizao.copiloto.utils.persistence.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CopilotoService : LifecycleService(), CopilotoSkin.ProximityListener,
    CopilotoMouth.SpeechRequester, LocationListener {
    private val binder = CopilotoBinder()

    private lateinit var copilotoEars: CopilotoEars
    private lateinit var copilotoMouth: CopilotoMouth
    private lateinit var copilotoEyes: CopilotoEyes
    private lateinit var copilotoSkin: CopilotoSkin

    private var locationManager: LocationManager? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var locationAvailable = false

    private val ttsEnabled = Preferences.booleanLiveData(TTS_ENABLED)

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        copilotoEyes.stopWatching()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CAMERA_START_ACTION -> {
                startWatching()
                Preferences.putBoolean(CAMERA_ON_BACKGROUND, true)
            }
            STT_LISTENING_ACTION -> {
                scheduleListening()
            }
            REQUEST_WATSON_ACTION -> {
                intent.extras?.let {
                    val text = it.getString(EXTRA_TEXT)
                    if (!text.isNullOrEmpty()) {
                        requestWatson(text)
                    }
                }
            }
            REQUEST_SPEECH_ACTION -> {
                intent.extras?.let {
                    val text = it.getString(EXTRA_TEXT)
                    if (!text.isNullOrEmpty()) {
                        requestSpeech(
                            ChatMessage(
                                type = MessageType.BOT.name,
                                text = text
                            ).apply { shouldAdd = false }
                        )
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        copilotoMouth = CopilotoMouth(this)
        ttsEnabled.observe(this) {
            copilotoMouth.init()
        }

        copilotoEars = CopilotoEars(this, this)
        copilotoEyes = CopilotoEyes(this, this)
        copilotoSkin = CopilotoSkin(this, this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                60000,
                10f,
                this
            )
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10f, this)
            val location = try {
                locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (ignored: Exception) {
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            latitude = location?.latitude ?: 0.0
            longitude = location?.longitude ?: 0.0
        } catch (e: Exception) {
            locationAvailable = false
        }

        ChatRepository.messages.observe(this) {
            val subList = it.subList(Preferences.getInt(SERVICE_MESSAGE_INDEX), it.size)
            subList.forEach { message ->
                Preferences.incrementInt(SERVICE_MESSAGE_INDEX)
                if (message.type == MessageType.USER.name) {
                    requestWatson(message.text)
                    copilotoEars.stopListening()
                }
            }
        }
    }

    override fun onDestroy() {
        copilotoEyes.stopWatching()
        copilotoMouth.release()
        copilotoEars.release()
        super.onDestroy()
    }

    override fun onToClose() {
        callAssistant()
    }

    override fun onRequestSpeech(chatMessage: ChatMessage) {
        requestSpeech(chatMessage)
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        IntentHelper.none(ChatMessage(type = "none"))
        super.onStatusChanged(provider, status, extras)
    }

    override fun onProviderEnabled(provider: String) {
        locationAvailable = true
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        locationAvailable = false
        super.onProviderDisabled(provider)
    }

    fun startWatching(texView: TextureView? = null, graphicOverlay: GraphicOverlay? = null) {
        copilotoEyes.init(this, texView, graphicOverlay)
        startForeground(
            Constants.CAMERA_CHANEL_ID,
            NotificationHelper.buildCameraNotification(this)
        )
    }

    private fun requestSpeech(chatMessage: ChatMessage, utturanceId: String = "ignored") {
        mouthSpeak(chatMessage, utturanceId)
        scheduleListening()
    }

    private fun mouthSpeak(chatMessage: ChatMessage, utturanceId: String) {
        copilotoMouth.speechQueue.add(Pair(chatMessage, utturanceId))
        if (copilotoEars.isListening) {
            scheduleSpeech()
        } else {
            copilotoMouth.processSpeechQueue()
        }
    }

    private fun callAssistant() {
        requestSpeech(
            ChatMessage(
                type = MessageType.BOT.name,
                text = "Oi, estou te escutando. O que você precisa?"
            )
        )
    }

    private fun requestWatson(text: String) {
        GlobalScope.launch {
            val response = try {
                handleWatsonResponse(
                    WatsonApi.retrofitService.getResponse(
                        WatsonRequest(
                            message = text,
                            lat = latitude,
                            lng = longitude
                        )
                    )
                )
            } catch (e: Exception) {
                listOf(
                    ChatMessage(
                        type = MessageType.BOT.name,
                        text = "Desculpa, estamos com algum problema de conexão"
                    )
                )
            }
            response.forEach {
                requestSpeech(it)
            }
        }
    }

    private fun handleWatsonResponse(watsonResponse: WatsonResponse): List<ChatMessage> {
        val messages = arrayListOf<ChatMessage>()
        watsonResponse.response.forEach {
            messages.add(ChatMessage(type = MessageType.BOT.name, text = it.text))
        }
        if (watsonResponse.isLocation) {
            messages.add(
                (ChatMessage(
                    type = MessageType.ANSWER.name,
                    confirmationAction = ConfirmationAction.NAVIGATION.name,
                    lat = watsonResponse.lat,
                    lng = watsonResponse.lng
                ))
            )
        }
        return messages
    }

    private fun scheduleListening() {
        if (!copilotoEars.isListening) {
            MainScope().launch {
                do {
                    delay(600)
                } while (copilotoMouth.isSpeaking)
                copilotoEars.starListening()
            }
        }
    }

    private fun scheduleSpeech() {
        MainScope().launch {
            do {
                delay(600)
            } while (copilotoEars.isListening)
            copilotoMouth.processSpeechQueue()
        }
    }

    companion object {

        var imageWidth = 480
        var imageHeight = 360
    }

    inner class CopilotoBinder : Binder() {
        fun getService(): CopilotoService = this@CopilotoService
    }
}