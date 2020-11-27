package br.pizao.copiloto.database

import androidx.lifecycle.LiveData
import androidx.room.*
import br.pizao.copiloto.database.model.ChatMessage

@Dao
interface ChatDatabaseDAO {

    @Insert
    @Transaction
    suspend fun insert(chatMessage: ChatMessage)

    @Query("SELECT * from chat_messages_table ORDER BY id ASC")
    fun getChatMessages(): LiveData<List<ChatMessage>>

    @Query("DELETE FROM chat_messages_table")
    fun clear()

    @Update
    fun update(chatMessage: ChatMessage)
}
