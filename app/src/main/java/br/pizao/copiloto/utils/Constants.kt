package br.pizao.copiloto.utils

object Constants {

    const val PERMISSION_REQUEST_CODE = 100
    const val TTS_DATA_CHECK_CODE = 888

    const val CAMERA_CHANEL_ID = 999

    const val CAMERA_START_ACTION = "br.pizao.copiloto.camera.START"
    const val STT_LISTENING_ACTION = "br.pizao.copiloto.camera.STT"

    const val CAMERA_STATUS = "camera_status_key"
    const val TTS_ENABLED = "tts_status_key"
    const val SERVICE_MESSAGE_INDEX = "service_message_index_key"
    const val WAITING_ANSWER = "waiting_answer_key"
    const val ANSWER = "answer_key"

    const val POSITIVE_ANSWER = "positive_answer"
    const val NEGATIVE_ANSWER = "negative_answer"

    val YES_LIST = listOf("Sim", "sim", "yes", "yeah", "ok", "okay")
    val NO_LIST = listOf("Não", "não", "nao", "Nao", "no")
}