package br.pizao.copiloto.database

import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.database.model.ChatMessage

object ChatRepository {
    private val chatDatabaseDao = ChatDatabase.getInstance(MainApplication.instance).chatDatabaseDAO

    val messages = chatDatabaseDao.getChatMessages()

    suspend fun insertMessage(chatMessage: ChatMessage) = chatDatabaseDao.insert(chatMessage)

    fun clearDatabase() = chatDatabaseDao.clear()
}