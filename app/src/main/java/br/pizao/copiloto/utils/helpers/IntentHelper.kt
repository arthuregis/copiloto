package br.pizao.copiloto.utils.helpers

import android.content.Intent
import android.net.Uri
import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.utils.Constants
import br.pizao.copiloto.utils.Constants.CAMERA_PREVIEW_ACTION
import br.pizao.copiloto.utils.Constants.NEGATIVE_ANSWER_ACTION
import br.pizao.copiloto.utils.Constants.POSITIVE_ANSEWR_ACTION
import br.pizao.copiloto.utils.Constants.REQUEST_SPEECH_ACTION
import br.pizao.copiloto.utils.Constants.REQUEST_WATSON_ACTION
import br.pizao.copiloto.utils.extensions.isCopilotoServiceRunning

object IntentHelper {
    private val context = MainApplication.instance

    fun requestNavigationApps(chatMessage: ChatMessage) {
        val pm = context.packageManager
        val latitude = chatMessage.lat
        val longitude = chatMessage.lng
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("waze://ul?ll=$latitude,$longitude&navigate=yes")
        ).let { wazeIntent ->
            wazeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (wazeIntent.resolveActivity(pm) != null) {
                context.startActivity(wazeIntent)
            } else {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=$latitude,$longitude")
                ).apply {
                    `package` = "com.google.android.apps.maps"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let {
                    if (it.resolveActivity(pm) != null) {
                        context.startActivity(it)
                    } else {
                        ChatRepository.addMessage(
                            ChatMessage(
                                answerRequired = false,
                                isUser = false,
                                text = "Desculpa, não encontrei nenhum aplicativo de navegação disponível"
                            )
                        )
                    }
                }
            }
        }
        ChatRepository.updateMessage(chatMessage.apply {
            answerRequired = false
            text = context.getString(R.string.button_yes)
        })
    }

    fun openCameraPreview(chatMessage: ChatMessage) {
        Intent().apply {
            action = CAMERA_PREVIEW_ACTION
        }.let {
            context.sendBroadcast(it)
        }
        ChatRepository.updateMessage(chatMessage.apply {
            answerRequired = false
            text = context.getString(R.string.button_yes)
        })
    }

    fun requestWatsonAssistance(chatMessage: ChatMessage) {
        Intent(context, CopilotoService::class.java).apply {
            action = REQUEST_WATSON_ACTION
            putExtra(Constants.EXTRA_TEXT, chatMessage.text)
        }.also {
            context.startService(it)
        }
        ChatRepository.updateMessage(chatMessage.apply {
            answerRequired = false
            text = context.getString(R.string.button_yes)
        })
    }

    fun sendPositiveAnswer() {
        Intent().apply {
            action = POSITIVE_ANSEWR_ACTION
        }.let {
            context.sendBroadcast(it)
        }
    }

    fun sendNegativeAnswer() {
        Intent().apply {
            action = NEGATIVE_ANSWER_ACTION
        }.let {
            context.sendBroadcast(it)
        }
    }

    fun startCopilotoService() {
        if(!context.isCopilotoServiceRunning()){
            Intent(context, CopilotoService::class.java).also {
                context.startService(it)
            }
        }
    }

    fun requestSpeech(text: String) {
        Intent(context, CopilotoService::class.java).apply {
            action = REQUEST_SPEECH_ACTION
            putExtra(Constants.EXTRA_TEXT, text)
        }.also {
            context.startService(it)
        }
    }

    fun none(ignored: ChatMessage) {}
}