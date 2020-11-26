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
import br.pizao.copiloto.utils.Constants.ANSWER
import br.pizao.copiloto.utils.Constants.NEGATIVE_ANSWER
import br.pizao.copiloto.utils.Constants.NO_LIST
import br.pizao.copiloto.utils.Constants.POSITIVE_ANSWER
import br.pizao.copiloto.utils.Constants.WAITING_ANSWER
import br.pizao.copiloto.utils.Constants.YES_LIST
import br.pizao.copiloto.utils.persistence.Preferences

class CopilotoEars(val context: Context, val listener: SpeechRequester) : RecognitionListenerImpl {

    private var speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@CopilotoEars)
        }

    var isListening = false; private set

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (Preferences.getBoolean(WAITING_ANSWER)) {
            matches?.let {
                val yes =
                    it.any { setence -> setence.split(" ").any { word -> YES_LIST.contains(word) } }
                val no =
                    it.any { setence -> setence.split(" ").any { word -> NO_LIST.contains(word) } }

                if (yes) {
                    Preferences.putString(ANSWER, POSITIVE_ANSWER)
                } else if (no) {
                    Preferences.putString(ANSWER, NEGATIVE_ANSWER)
                }
            }
            listener.onRequestSpeech(
                ChatMessage(
                    false,
                    isUser = false,
                    text = "Não identifiquei sua resposta, você pode repitir se desejar."
                ).apply { shouldAdd = false }
            )
        } else {
            matches?.first()?.let {
                if (it.isNotEmpty()) {
                    ChatRepository.addMessage(
                        ChatMessage(
                            answerRequired = false,
                            isUser = true,
                            text = it
                        )
                    )
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

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun release() {
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
    }

    interface SpeechRequester {
        fun onRequestSpeech(chatMessage: ChatMessage)
    }
}