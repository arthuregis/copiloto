package br.pizao.copiloto.database.model

data class ChatMessage(
    val isUser: Boolean,
    val text: String
)