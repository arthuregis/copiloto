package br.pizao.copiloto.utils

object Constants {

    const val PERMISSION_REQUEST_CODE = 100
    const val TTS_DATA_CHECK_CODE = 888

    const val CAMERA_CHANEL_ID = 999

    const val CAMERA_START_ACTION = "br.pizao.copiloto.service.eyes.START"
    const val STT_LISTENING_ACTION = "br.pizao.copiloto.service.ears.START"
    const val REQUEST_SPEECH_ACTION = "br.pizao.copiloto.service.mouth.REQUEST"
    const val REQUEST_WATSON_ACTION = "br.pizao.copiloto.service.watson.REQUEST"
    const val CAMERA_PREVIEW_ACTION = "br.pizao.copiloto.main.preview.START"
    const val POSITIVE_ANSEWR_ACTION = "br.pizao.copiloto.main.answer.POSITIVE"
    const val NEGATIVE_ANSWER_ACTION = "br.pizao.copiloto.main.ansert.NEGATIVE"

    const val CAMERA_STATUS = "camera_status_key"
    const val TTS_ENABLED = "tts_status_key"
    const val SERVICE_MESSAGE_INDEX = "service_message_index_key"
    const val WAITING_ANSWER = "waiting_answer_key"
    const val CAMERA_ON_BACKGROUND = "camera_on_back_ground_key"
    const val SENSOR_ENABLED = "sensor_enabled_key"

    const val EXTRA_TEXT = "extra_text_key"

    val YES_LIST = listOf("Sim", "sim", "yes", "yeah", "ok", "okay")
    val NO_LIST = listOf("Não", "não", "nao", "Nao", "no")
}