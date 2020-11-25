package br.pizao.copiloto.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.TextureView
import androidx.lifecycle.LifecycleService
import br.pizao.copiloto.ui.overlay.GraphicOverlay
import br.pizao.copiloto.utils.Constants
import br.pizao.copiloto.utils.Constants.CAMERA_START_ACTION
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.Constants.TTS_ENABLED
import br.pizao.copiloto.utils.Constants.TTS_SPEAK_ACTION
import br.pizao.copiloto.utils.Constants.TTS_TEXT_KEY
import br.pizao.copiloto.utils.helpers.NotificationHelper
import br.pizao.copiloto.utils.persistence.Preferences
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CopilotoService : LifecycleService(), CopilotoSkin.ProximityListener {
    private val binder = CameraBinder()

    private lateinit var copilotoEars: CopilotoEars
    private lateinit var copilotoMouth: CopilotoMouth
    private lateinit var copilotoEyes: CopilotoEyes
    private lateinit var copilotoSkin: CopilotoSkin

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
            }
            TTS_SPEAK_ACTION -> {
                requestSpeech(intent.extras?.get(TTS_TEXT_KEY).toString())
            }
            STT_LISTENING_ACTION -> {
                copilotoEars.starListening()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        copilotoMouth = CopilotoMouth(this)
        ttsEnabled.observe(this) {
            copilotoMouth.init()
        }

        copilotoEars = CopilotoEars(this)
        copilotoEyes = CopilotoEyes(this)
        copilotoSkin = CopilotoSkin(this, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        copilotoEyes.stopWatching()
        copilotoMouth.release()
        copilotoEars.release()
    }

    fun startWatching(texView: TextureView? = null, graphicOverlay: GraphicOverlay? = null){
        copilotoEyes.init(texView, graphicOverlay)
        startForeground(Constants.CAMERA_CHANEL_ID, NotificationHelper.buildCameraNotification(this))
    }

    private fun requestSpeech(text: String, utturanceId: String = "tts") {
        if (mouthSpeak(text, utturanceId)) {
            MainScope().launch {
                do {
                    delay(500)
                } while (copilotoMouth.isSpeaking)
                copilotoEars.starListening()
            }
        }
    }

    private fun mouthSpeak(text: String, utturanceId: String = "tts"): Boolean {
        if (text == "null") return false
        if (!copilotoMouth.isSpeaking && !copilotoEars.isListening) {
            return copilotoMouth.speak(text, utturanceId)
        }
        return false
    }

    private fun callAssistant() {
        requestSpeech("VocÊ me chamou?", TRIGGER_ID)
    }

    inner class CameraBinder : Binder() {
        fun getService(): CopilotoService = this@CopilotoService
    }

    companion object {
        private const val TRIGGER_ID = "trigger_id"
        var imageWidth = 480
        var imageHeight = 360
    }

    override fun onToClose() {
        callAssistant()
    }
}
