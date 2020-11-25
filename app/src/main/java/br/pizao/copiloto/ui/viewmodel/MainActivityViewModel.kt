package br.pizao.copiloto.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.utils.Constants.CAMERA_STATUS
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.persistence.Preferences
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    val messages = ChatRepository.messages

    val isCameraOn = Preferences.booleanLiveData(CAMERA_STATUS)

    val requestText = MutableLiveData("")

    val chatIcon = Transformations.map(requestText) {
        if (requestText.value.isNullOrEmpty()) {
            R.drawable.round_mic_black_36
        } else {
            R.drawable.round_send_black_36
        }
    }

    fun onButtonClicked() {
        if (requestText.value.isNullOrEmpty()) {
            requestSpeechListening()
        } else {
            addMessageToDatabase()
        }
    }

    private fun requestSpeechListening() {
        Intent(context, CopilotoService::class.java).apply {
            action = STT_LISTENING_ACTION
        }.also {
            context.startService(it)
        }
    }

    private fun addMessageToDatabase() {
        requestText.value?.let { text ->
            viewModelScope.launch {
                ChatRepository.insertMessage(ChatMessage(true, text))
            }
            requestText.value = ""
        }
    }
}