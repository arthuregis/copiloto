package br.pizao.copiloto.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import br.pizao.copiloto.R
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.Constants.STT_SHARED_KEY
import br.pizao.copiloto.utils.persistence.Preferences

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    val requestText = MutableLiveData("")
    val sstText = Preferences.stringLiveData(STT_SHARED_KEY)

    private var _addMessageTrigger = MutableLiveData(false)
    val addMessageTrigger = _addMessageTrigger

    val chatIcon = Transformations.map(requestText) {
        if (requestText.value.isNullOrEmpty()) {
            R.drawable.round_mic_black_36
        } else {
            R.drawable.round_send_black_36
        }
    }

    init {
        Preferences.putString(STT_SHARED_KEY, "")
    }

    fun onButtonClicked() {
        if(requestText.value.isNullOrEmpty()){
            requestSpeechListening()
        } else {
            _addMessageTrigger.value = true
        }
    }

    private fun requestSpeechListening() {
        Intent(context, CopilotoService::class.java).apply {
            action = STT_LISTENING_ACTION
        }.also {
            context.startService(it)
        }
    }

    fun addMessageCompleted() {
        _addMessageTrigger.value = false
    }
}