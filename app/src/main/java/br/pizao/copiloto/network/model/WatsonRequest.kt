package br.pizao.copiloto.network.model

data class WatsonRequest(
    val message: String,
    val userId: String = "1",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)