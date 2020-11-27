package br.pizao.copiloto.database

import android.content.Context
import androidx.lifecycle.LiveData
import br.pizao.copiloto.database.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ChatRepository {
    private lateinit var chatDatabaseDao: ChatDatabaseDAO

    lateinit var messages: LiveData<List<ChatMessage>>; private set

    var controlledId: Long = 0

    fun init(context: Context) {
        chatDatabaseDao = ChatDatabase.getInstance(context).chatDatabaseDAO
        messages = chatDatabaseDao.getChatMessages()
    }

    fun addMessage(chatMessage: ChatMessage) {
        synchronized(this) {
            chatMessage.id = controlledId++
            CoroutineScope(Dispatchers.IO).launch {
                insertMessage(chatMessage)
            }
        }
    }

    fun updateMessage(chatMessage: ChatMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            chatDatabaseDao.update(chatMessage)
        }
    }

    fun clearDatabase() = chatDatabaseDao.clear()

    private suspend fun insertMessage(chatMessage: ChatMessage) =
        chatDatabaseDao.insert(chatMessage)
}
