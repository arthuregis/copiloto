package br.pizao.copiloto.service.impl

import android.os.Bundle
import android.speech.RecognitionListener

interface RecognitionListenerImpl : RecognitionListener {

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onReadyForSpeech(p0: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(p0: Float) {}

    override fun onBufferReceived(p0: ByteArray?) {}

    override fun onEndOfSpeech() {}

    override fun onEvent(p0: Int, p1: Bundle?) {}
}