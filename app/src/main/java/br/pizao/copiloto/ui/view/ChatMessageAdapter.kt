package br.pizao.copiloto.ui.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.pizao.copiloto.R
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.databinding.ItemMessageAnswerBinding
import br.pizao.copiloto.databinding.ItemMessageBotBinding
import br.pizao.copiloto.databinding.ItemMessageUserBinding
import br.pizao.copiloto.utils.Constants.WAITING_ANSWER
import br.pizao.copiloto.utils.persistence.Preferences

class ChatMessageAdapter : RecyclerView.Adapter<BindingViewHolder>() {

    private val chatMessages = arrayListOf<ChatMessage>()
    private var lastMessageHolder: BindingViewHolder? = null

    val lastPosition: Int
        get() = chatMessages.size - 1

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
            1 -> BotMessageViewHolder(
                ItemMessageBotBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            else -> AnswerViewHolder(
                ItemMessageAnswerBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        holder.bind(chatMessages[position])
        lastMessageHolder = holder
    }


    override fun getItemCount(): Int = chatMessages.size

    override fun getItemViewType(position: Int): Int {
        val message = chatMessages[position]
        return when(MessageType.valueOf(message.type)) {
            MessageType.USER -> 0
            MessageType.BOT -> 1
            MessageType.ANSWER -> 2
        }

    }

    fun addChatMessage(message: ChatMessage) {
        if (message.type == MessageType.ANSWER.name) {
            Preferences.putBoolean(WAITING_ANSWER, true)
        } else {
            Preferences.putBoolean(WAITING_ANSWER, false)
        }
        if(message.type != MessageType.ANSWER.name || chatMessages[lastPosition].type != MessageType.ANSWER.name) {
            chatMessages.add(message)
            notifyItemInserted(lastPosition)
        }
    }

    fun confirmAction() {
        doAction(R.id.yes_button)
    }

    fun refuseAction() {
        doAction(R.id.no_button)
    }

    private fun doAction(id: Int) {
        if (lastMessageHolder is AnswerViewHolder) {
            (lastMessageHolder as AnswerViewHolder).clickButton(id)
            notifyDataSetChanged()
        }
    }
}