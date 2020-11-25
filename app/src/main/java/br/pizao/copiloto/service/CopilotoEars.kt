package br.pizao.copiloto.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.service.impl.RecognitionListenerImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class CopilotoEars(val context: Context) : RecognitionListenerImpl {

    private var speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@CopilotoEars)
        }

    var isListening = false; private set

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        MainScope().launch {
            matches?.first()?.let {
                if (it.isNotEmpty()) {
                    ChatRepository.insertMessage(ChatMessage(true, it))
                }
            }
        }

        val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        matches?.let {
            Log.d(javaClass.name, "recognitionListener.onResults ${it.joinToString()}")
        }
        scores?.let {
            Log.d(javaClass.name, "recognitionListener.onResults ${it.joinToString()}")
        }
        isListening = false
    }

    override fun onError(error: Int) {
        val mError = when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> " network timeout"
            SpeechRecognizer.ERROR_NETWORK -> " network"
            SpeechRecognizer.ERROR_AUDIO -> " audio"
            SpeechRecognizer.ERROR_SERVER -> " server"
            SpeechRecognizer.ERROR_CLIENT -> " client"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> " speech time out"
            SpeechRecognizer.ERROR_NO_MATCH -> " no match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> " recogniser busy"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> " insufficient permissions"
            else -> "unknown error"
        }
        Log.d(javaClass.name, "recognitionListener.onError $mError")
        isListening = false
    }

    fun starListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            isListening = true
            speechRecognizer.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        context.packageName
                    )
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                })
        }
    }

    fun release() {
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
    }
}