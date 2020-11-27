package br.pizao.copiloto.ui.view

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.ConfirmationAction
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.databinding.ItemMessageAnswerBinding
import br.pizao.copiloto.databinding.ItemMessageBotBinding
import br.pizao.copiloto.databinding.ItemMessageUserBinding
import br.pizao.copiloto.utils.Constants.WAITING_ANSWER
import br.pizao.copiloto.utils.persistence.Preferences

class UserMessageViewHolder(private val userMessage: ItemMessageUserBinding) :
    BindingViewHolder(userMessage.root) {

    override fun bind(chatMessage: ChatMessage) {
        userMessage.message = chatMessage
    }
}


class BotMessageViewHolder(val botMessage: ItemMessageBotBinding) :
    BindingViewHolder(botMessage.root) {

    override fun bind(chatMessage: ChatMessage) {
        botMessage.message = chatMessage
    }
}

class AnswerViewHolder(private val answerMessage: ItemMessageAnswerBinding) :
    BindingViewHolder(answerMessage.root) {

    override fun bind(chatMessage: ChatMessage) {
        val textField = answerMessage.root.findViewById<TextView>(R.id.answer_text)
        val noButton = answerMessage.root.findViewById<Button>(R.id.no_button)
        val yesButton = answerMessage.root.findViewById<Button>(R.id.yes_button)
        val container = answerMessage.root.findViewById<ConstraintLayout>(R.id.answer_container)

        if (chatMessage.text !in listOf("Sim", "Yes", "No", "no")) {
            textField.text = ""
            noButton.visibility = View.VISIBLE
            yesButton.visibility = View.VISIBLE
            container.setPadding(answerMessage.root.resources.getDimension(R.dimen._7sdp).toInt())
        }

        noButton.setOnClickListener {
            textField.setText(R.string.button_no)
            noButton.visibility = View.GONE
            yesButton.visibility = View.GONE
            container.setPadding(0)
            ChatRepository.updateMessage(chatMessage.apply {
                type = MessageType.USER.name
                text = textField.text.toString()
            })
            Preferences.putBoolean(WAITING_ANSWER, false)
        }

        yesButton.setOnClickListener {
            textField.setText(R.string.button_yes)
            noButton.visibility = View.GONE
            yesButton.visibility = View.GONE
            container.setPadding(0)
            ConfirmationAction.valueOf(chatMessage.confirmationAction)
                .action(chatMessage)
            Preferences.putBoolean(WAITING_ANSWER, false)
        }
    }

    override fun clickButton(id: Int) {
        super.clickButton(id)
        itemView.findViewById<Button>(id)?.performClick()
    }

}

abstract class BindingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(chatMessage: ChatMessage)
    open fun clickButton(id: Int) {}
}
