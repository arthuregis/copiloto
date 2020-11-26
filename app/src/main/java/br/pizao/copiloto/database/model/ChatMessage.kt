package br.pizao.copiloto.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages_table")
data class ChatMessage(
    @ColumnInfo(name = "answer_required")
    val answerRequired: Boolean,

    @ColumnInfo(name = "is_user")
    val isUser: Boolean = true,

    @ColumnInfo(name = "text")
    val text: String = "",

    @ColumnInfo(name = "latitude")
    val lat: Double = 0.0,

    @ColumnInfo(name = "longitude")
    val lng: Double = 0.0,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
){
    var shouldAdd = true
}