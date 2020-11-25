package br.pizao.copiloto.database.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages_table")
data class ChatMessage(

    @ColumnInfo(name = "is_user")
    @NonNull
    val isUser: Boolean,

    @ColumnInfo(name = "text")
    @NonNull
    val text: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
)