package br.pizao.copiloto.ui.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import br.pizao.copiloto.databinding.ItemMessageBotBinding
import br.pizao.copiloto.databinding.ItemMessageUserBinding
import br.pizao.copiloto.database.model.ChatMessage

class UserMessageViewHolder(val userMessage: ItemMessageUserBinding) : BindingViewHolder(userMessage.root) {
    override fun bind(chatMessage: ChatMessage) {
        userMessage.message = chatMessage
    }
}


class BotMessageViewHolder(val botMessage: ItemMessageBotBinding) : BindingViewHolder(botMessage.root) {
    override fun bind(chatMessage: ChatMessage) {
        botMessage.message = chatMessage
    }
}

abstract class BindingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(chatMessage: ChatMessage)
}
