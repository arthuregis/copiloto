package br.pizao.copiloto.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.pizao.copiloto.utils.helpers.IntentHelper

@Entity(tableName = "chat_messages_table")
data class ChatMessage(
    @ColumnInfo(name = "answer_required")
    var answerRequired: Boolean,
    @ColumnInfo(name = "confirmation_action")
    val confirmationAction: String = "NONE",
    @ColumnInfo(name = "is_user")
    val isUser: Boolean = true,
    @ColumnInfo(name = "text")
    var text: String = "",
    @ColumnInfo(name = "latitude")
    val lat: Double = 0.0,
    @ColumnInfo(name = "longitude")
    val lng: Double = 0.0,
    @PrimaryKey
    var id: Long = 0L
) {
    var shouldAdd = true
    var addRequestForHelp = false
}

enum class ConfirmationAction(val action: (ChatMessage) -> Unit) {
    NAVIGATION(IntentHelper::requestNavigationApps),
    CAMERA(IntentHelper::openCameraPreview),
    REQUESTHELP(IntentHelper::requestWatsonAssistance),
    NONE(IntentHelper::none)
}


