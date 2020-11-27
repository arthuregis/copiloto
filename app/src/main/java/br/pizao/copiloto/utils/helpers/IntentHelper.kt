package br.pizao.copiloto.utils.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    private lateinit var mContext: Context

    fun init(context: Context){
        if(!::mContext.isInitialized){
            mContext = context
        }
    }

    fun requestNavigationApps(chatMessage: ChatMessage) {
        val pm = mContext.packageManager
        val latitude = chatMessage.lat
        val longitude = chatMessage.lng
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("waze://ul?ll=$latitude,$longitude&navigate=yes")
        ).let { wazeIntent ->
            wazeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (wazeIntent.resolveActivity(pm) != null) {
                mContext.startActivity(wazeIntent)
            } else {
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=$latitude,$longitude")
                ).apply {
                    `package` = "com.google.android.apps.maps"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }.let {
                    if (it.resolveActivity(pm) != null) {
                        mContext.startActivity(it)
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
            text = mContext.getString(R.string.button_yes)
        })
    }

    fun openCameraPreview(chatMessage: ChatMessage) {
        Intent().apply {
            action = CAMERA_PREVIEW_ACTION
        }.let {
            mContext.sendBroadcast(it)
        }
        ChatRepository.updateMessage(chatMessage.apply {
            answerRequired = false
            text = mContext.getString(R.string.button_yes)
        })
    }

    fun requestWatsonAssistance(chatMessage: ChatMessage) {
        Intent(mContext, CopilotoService::class.java).apply {
            action = REQUEST_WATSON_ACTION
            putExtra(Constants.EXTRA_TEXT, chatMessage.text)
        }.also {
            mContext.startService(it)
        }
        ChatRepository.updateMessage(chatMessage.apply {
            answerRequired = false
            text = mContext.getString(R.string.button_yes)
        })
    }

    fun sendPositiveAnswer() {
        Intent().apply {
            action = POSITIVE_ANSEWR_ACTION
        }.let {
            mContext.sendBroadcast(it)
        }
    }

    fun sendNegativeAnswer() {
        Intent().apply {
            action = NEGATIVE_ANSWER_ACTION
        }.let {
            mContext.sendBroadcast(it)
        }
    }

    fun startCopilotoService() {
        if(!mContext.isCopilotoServiceRunning()){
            Intent(mContext, CopilotoService::class.java).also {
                mContext.startService(it)
            }
        }
    }

    fun requestSpeech(text: String) {
        Intent(mContext, CopilotoService::class.java).apply {
            action = REQUEST_SPEECH_ACTION
            putExtra(Constants.EXTRA_TEXT, text)
        }.also {
            mContext.startService(it)
        }
    }

    fun none(ignored: ChatMessage) {}
}