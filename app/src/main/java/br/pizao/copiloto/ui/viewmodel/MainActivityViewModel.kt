package br.pizao.copiloto.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.utils.Constants.CAMERA_STATUS
import br.pizao.copiloto.utils.Constants.STT_LISTENING_ACTION
import br.pizao.copiloto.utils.extensions.isCopilotoServiceRunning
import br.pizao.copiloto.utils.helpers.IntentHelper
import br.pizao.copiloto.utils.persistence.Preferences

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
        IntentHelper.startCopilotoService()

        requestText.value?.let { text ->
            ChatRepository.addMessage(
                ChatMessage(
                    type = MessageType.USER.name,
                    text = text
                )
            )
            requestText.value = ""
        }
    }
}