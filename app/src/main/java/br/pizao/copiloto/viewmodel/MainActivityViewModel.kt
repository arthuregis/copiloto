package br.pizao.copiloto.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import br.pizao.copiloto.R
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.Constants.STT_SHARED_KEY
import br.pizao.copiloto.utils.Constants.TTS_SPEAK_ACTION
import br.pizao.copiloto.utils.Constants.TTS_TEXT_KEY
import br.pizao.copiloto.utils.Preferences
import br.pizao.copiloto.utils.Preferences.CAMERA_STATUS

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    val cameraButtonText: LiveData<Int> =
        Transformations.map(Preferences.booleanLiveData(CAMERA_STATUS)) {
            if (it) {
                R.string.camera_stop_text
            } else {
                R.string.camera_start_text
            }
        }

    val ttsText = MutableLiveData<String>()
    val sttText = Preferences.stringLiveData(STT_SHARED_KEY)

    private val _openCameraTrigger = MutableLiveData(false)
    val openCameraTrigger: LiveData<Boolean>
        get() = _openCameraTrigger

    fun onCameraButtonClicked() {
        when (cameraButtonText.value) {
            R.string.camera_start_text -> startCamera()
            R.string.camera_stop_text -> stopCamera()
        }
    }

    fun openCameraCompleted() {
        _openCameraTrigger.value = false
    }

    fun requestTTSSpeak() {
        Intent(context, CopilotoService::class.java).apply {
            action = TTS_SPEAK_ACTION
            putExtra(TTS_TEXT_KEY, ttsText.value)
        }.also {
            context.startService(it)
        }
    }

    fun requestSpeechListening(){
        Intent(context, CopilotoService::class.java).apply {
            action = STT_LISTENING_ACTION
        }.also {
            context.startService(it)
        }
    }

    private fun startCamera() {
        _openCameraTrigger.value = true
    }

    private fun stopCamera() {
        if (Preferences.getBoolean(CAMERA_STATUS)) {
            context.stopService(Intent(context, CopilotoService::class.java))
        }
    }
}