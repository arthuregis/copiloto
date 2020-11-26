package br.pizao.copiloto.ui.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.pizao.copiloto.R
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.databinding.ItemMessageAnswerBinding
import br.pizao.copiloto.databinding.ItemMessageBotBinding
import br.pizao.copiloto.databinding.ItemMessageUserBinding
import br.pizao.copiloto.utils.Constants
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
        return if (message.answerRequired) 2 else if (message.isUser) 0 else 1

    }

    fun addChatMessage(message: ChatMessage) {
        if(message.answerRequired) {
            Preferences.putBoolean(WAITING_ANSWER, true)
        } else {
            Preferences.putBoolean(WAITING_ANSWER, false)
        }
        chatMessages.add(message)
        notifyItemInserted(lastPosition)
    }

    fun handleAnswer(answer: String) {
        if(Preferences.getBoolean(WAITING_ANSWER) && (lastMessageHolder is AnswerViewHolder)){
            if(answer == Constants.POSITIVE_ANSWER){
                (lastMessageHolder as AnswerViewHolder).clickButton(R.id.yes_button)
            } else if(answer == Constants.NEGATIVE_ANSWER) {
                (lastMessageHolder as AnswerViewHolder).clickButton(R.id.no_button)
            }
        }
    }
}