package br.pizao.copiloto.utils.helpers

import android.content.Intent
import android.net.Uri
import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage

object IntentHelper {
    fun requestNavigationApps(latitude: Double, longitude: Double) {
        val context = MainApplication.instance
        val pm = context.packageManager
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
    }
}