package br.pizao.copiloto.network.model

import com.squareup.moshi.Json

data class WatsonResponse(
    val response: List<WatsonMessage>,
    val isLocation: Boolean,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class WatsonMessage(
    @Json(name = "response_type") val responseType: String,
    val text: String
)