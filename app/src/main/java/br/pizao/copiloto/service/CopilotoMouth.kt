package br.pizao.copiloto.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import br.pizao.copiloto.R
import br.pizao.copiloto.manager.CopilotoAudioManager
import java.util.*

class CopilotoMouth(val context: Context) : UtteranceProgressListener(),
    TextToSpeech.OnInitListener {

    private var mouth: TextToSpeech? = null

    var isSpeaking = false; private set

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
        isSpeaking = true
        CopilotoAudioManager.setVolumetoMax()
    }

    override fun onDone(utteranceId: String?) {
        isSpeaking = false
        CopilotoAudioManager.resetVolume()
    }

    override fun onError(p0: String?) {
        isSpeaking = false
    }

    override fun onError(utteranceId: String, errorCode: Int) {
        isSpeaking = false
        CopilotoAudioManager.resetVolume()
    }

    fun speak(text: String, utturanceId: String = "tts"): Boolean {
        mouth?.let {
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, utturanceId)
            return true
        }
        return false
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

}