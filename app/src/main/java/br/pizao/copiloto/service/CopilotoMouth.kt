package br.pizao.copiloto.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.ConfirmationAction
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.manager.CopilotoAudioManager
import java.util.*

class CopilotoMouth(val context: Context) : UtteranceProgressListener(),
    TextToSpeech.OnInitListener {

    private var mouth: TextToSpeech? = null

    var speechQueue = LinkedList<Pair<ChatMessage, String>>()

    var isSpeaking = false;private set
        get() = mouth?.isSpeaking == true

    override fun onInit(initStatus: Int) {
        if (initStatus == TextToSpeech.SUCCESS) {
            var result = mouth?.setLanguage(Locale("pt", "BR"))
            if (result in listOf(
                    TextToSpeech.LANG_MISSING_DATA,
                    TextToSpeech.LANG_NOT_SUPPORTED
                )
            ) {
                result = mouth?.setLanguage(Locale("pt", "POR"))
                if (result in listOf(
                        TextToSpeech.LANG_MISSING_DATA,
                        TextToSpeech.LANG_NOT_SUPPORTED
                    )
                ) {
                    Toast.makeText(
                        context,
                        R.string.tts_pt_not_supported,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                context,
                R.string.tts_init_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onStart(utteranceId: String?) {
        CopilotoAudioManager.setVolumetoMax()
    }

    override fun onDone(utteranceId: String?) {
        CopilotoAudioManager.resetVolume()
    }

    override fun onError(p0: String?) {

    }

    override fun onError(utteranceId: String, errorCode: Int) {

        CopilotoAudioManager.resetVolume()
    }

    fun init() {
        mouth = TextToSpeech(context, this).apply {
            setOnUtteranceProgressListener(this@CopilotoMouth)
        }
    }

    fun release() {
        mouth?.stop()
        mouth?.shutdown()
    }

    fun processSpeechQueue() {
        while (speechQueue.isNotEmpty()) {
            if(speechQueue.size > 2) {
                speechQueue = LinkedList<Pair<ChatMessage, String>>(speechQueue.subList(2,speechQueue.size))
            }
            val speech = speechQueue.poll()
            speak(speech.first, speech.second)
        }
    }

    private fun speak(chatMessage: ChatMessage, utturanceId: String) {
        mouth?.let {
            it.speak(chatMessage.text, TextToSpeech.QUEUE_ADD, null, utturanceId)
            if (chatMessage.shouldAdd) {
                ChatRepository.addMessage(
                    chatMessage
                )
            }
            if (chatMessage.addRequestForHelp) {
                ChatRepository.addMessage(
                    ChatMessage(
                        type = MessageType.ANSWER.name,
                        confirmationAction = ConfirmationAction.REQUESTHELP.name,
                        text = "Ache um lugar perto para que eu possa fazer uma parada"
                    )
                )
            }
        }
    }

    interface SpeechRequester {
        fun onRequestSpeech(chatMessage: ChatMessage)
    }
}