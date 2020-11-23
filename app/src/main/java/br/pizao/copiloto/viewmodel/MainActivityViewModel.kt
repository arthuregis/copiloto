package br.pizao.copiloto.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import br.pizao.copiloto.R
import br.pizao.copiloto.service.CameraService
import br.pizao.copiloto.utils.Preferences
import br.pizao.copiloto.utils.Preferences.CAMERA_STATUS

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    val cameraButtonText: LiveData<Int> =
        Transformations.map(Preferences.booleanLiveData(CAMERA_STATUS)) {
            if (it) {
                R.string.stop
            } else {
                R.string.start
            }
        }

    private val _openCameraTrigger = MutableLiveData(false)
    val openCameraTrigger: LiveData<Boolean>
        get() = _openCameraTrigger

    fun onCameraButtonClicked() {
        when (cameraButtonText.value) {
            R.string.start -> startCamera()
            R.string.stop -> stopCamera()
        }
    }

    fun openCameraCompleted() {
        _openCameraTrigger.value = false
    }

    private fun startCamera() {
        _openCameraTrigger.value = true
    }

    private fun stopCamera() {
        if (Preferences.getBoolean(CAMERA_STATUS)) {
            context.stopService(Intent(context, CameraService::class.java))
        }
    }
}