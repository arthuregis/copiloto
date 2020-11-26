package br.pizao.copiloto.database

import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.database.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ChatRepository {
    private val chatDatabaseDao = ChatDatabase.getInstance(MainApplication.instance).chatDatabaseDAO

    val messages = chatDatabaseDao.getChatMessages()

    fun addMessage(chatMessage: ChatMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            insertMessage(chatMessage)
        }
    }

    fun clearDatabase() = chatDatabaseDao.clear()

    private suspend fun insertMessage(chatMessage: ChatMessage) =
        chatDatabaseDao.insert(chatMessage)
}
