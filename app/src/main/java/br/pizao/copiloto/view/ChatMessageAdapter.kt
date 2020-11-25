package br.pizao.copiloto.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.pizao.copiloto.databinding.ItemMessageBotBinding
import br.pizao.copiloto.databinding.ItemMessageUserBinding
import br.pizao.copiloto.model.ChatMessage

class ChatMessageAdapter : RecyclerView.Adapter<BindingViewHolder>() {

    private val chatMessages = arrayListOf<ChatMessage>()
    val lastPosition: Int
        get() =  chatMessages.size - 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> UserMessageViewHolder(
                ItemMessageUserBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            else -> BotMessageViewHolder(
                ItemMessageBotBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        holder.bind(chatMessages[position])
    }

    override fun getItemCount(): Int = chatMessages.size

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].isUser) 0 else 1
    }

    fun addChatMessage(message: ChatMessage) {
        chatMessages.add(message)
        notifyItemInserted(lastPosition)
    }
}