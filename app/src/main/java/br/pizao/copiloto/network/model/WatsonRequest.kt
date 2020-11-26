package br.pizao.copiloto.network.model

data class WatsonRequest(
    val message: String,
    val userId: String = "1"
)