package br.pizao.copiloto.service

import android.graphics.PointF
import android.media.Image
import android.os.Handler
import android.text.format.DateUtils
import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.ConfirmationAction
import br.pizao.copiloto.manager.CopilotoAudioManager
import br.pizao.copiloto.ui.overlay.FaceGraphic
import br.pizao.copiloto.ui.overlay.GraphicOverlay
import br.pizao.copiloto.utils.Constants.CAMERA_ON_BACKGROUND
import br.pizao.copiloto.utils.persistence.Preferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorProcessor(
    private val listener: CopilotoMouth.SpeechRequester,
    private val graphicOverlay: GraphicOverlay? = null
) {

    private var isProcessing = false
    private var lastTimeEyeOpen = System.currentTimeMillis()
    private var lastTimeWithoutFace = System.currentTimeMillis()

    private val detector = FaceDetectorOptions.Builder()
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build().let { faceDetectorOptions ->
            FaceDetection.getClient(faceDetectorOptions)
        }

    private val handler = Handler(MainApplication.instance.mainLooper)
    private var assistRequestFlag = true

    fun processImage(image: Image, orientation: Int, closeCallback: Runnable) {
        if (!isProcessing) {
            isProcessing = true
            synchronized(this) {
                detector.process(InputImage.fromMediaImage(image, orientation))
                    .addOnSuccessListener { faces ->
                        graphicOverlay?.clear()
                        if (faces.isNotEmpty()) {
                            val face = faces[0]
                            checkEyes(face)
                            graphicOverlay?.add(FaceGraphic(graphicOverlay, face))
                            updateLastTimeWithoutFace()
                        } else {
                            updateLastTimeEyeOpen()
                            if (assistRequestFlag && Preferences.getBoolean(CAMERA_ON_BACKGROUND) &&
                                System.currentTimeMillis() - lastTimeWithoutFace > 10 * DateUtils.SECOND_IN_MILLIS
                            ) {
                                assistRequestFlag = false
                                handler.postDelayed({
                                    assistRequestFlag = true
                                }, DateUtils.MINUTE_IN_MILLIS * 3)

                                listener.onRequestSpeech(ChatMessage(
                                    answerRequired = false,
                                    isUser = false,
                                    text = "Olá, já faz um tempo que não tenho contato visual com você. Está tudo certo? Posso te ajudar em algo?"
                                ).apply { shouldAdd = false })
                            }
                        }
                        graphicOverlay?.postInvalidate()
                    }
                    .addOnCompleteListener {
                        closeCallback.run()
                        isProcessing = false
                    }.addOnFailureListener {
                        updateLastTimeEyeOpen()
                    }
            }
        } else {
            closeCallback.run()
        }
    }

    fun close() {
        detector.close()
    }

    private fun checkEyes(face: Face) {
        when (face.headEulerAngleY) {
            in EULER_ANGLEY_LIMIT..Float.MAX_VALUE -> {
                face.leftEyeOpenProbability?.let {
                    if (it > MIN_PROBABILITY) {
                        updateLastTimeEyeOpen()
                    }
                }
            }
            in -EULER_ANGLEY_LIMIT..EULER_ANGLEY_LIMIT -> {
                if (face.leftEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
                    val leftEyePoint = face.getContour(FaceContour.LEFT_EYE)?.points?.get(4)
                        ?: PointF(0F, 0F)
                    val rightEyePoint = face.getContour(FaceContour.RIGHT_EYE)?.points?.get(4)
                        ?: PointF(CopilotoService.imageWidth.toFloat(), 0F)
                    if ((face.leftEyeOpenProbability!! > MIN_PROBABILITY &&
                                leftEyePoint.x > 0 && leftEyePoint.y > 0)
                        || (face.rightEyeOpenProbability!! > MIN_PROBABILITY &&
                                rightEyePoint.x < CopilotoService.imageWidth &&
                                rightEyePoint.x > 0)
                    ) {
                        updateLastTimeEyeOpen()
                    }
                }
            }
            in -Float.MAX_VALUE..-EULER_ANGLEY_LIMIT -> {
                face.rightEyeOpenProbability?.let {
                    if (it > MIN_PROBABILITY) {
                        updateLastTimeEyeOpen()
                    }
                }
            }
        }

        if (System.currentTimeMillis() - lastTimeEyeOpen > 2 * DateUtils.SECOND_IN_MILLIS) {
            updateLastTimeEyeOpen()
            CopilotoAudioManager.horn()
            handler.postDelayed({
                listener.onRequestSpeech(
                    ChatMessage(
                        answerRequired = false,
                        isUser = false,
                        text = "Oi, você está mostrando sinais de cansaço. Posso buscar um local para você fazer uma parada?"
                    ).apply { addRequestForHelp = true }
                )
            }, (DateUtils.SECOND_IN_MILLIS * 2.5).toLong())

        }
    }

    private fun updateLastTimeEyeOpen() {
        lastTimeEyeOpen = System.currentTimeMillis()
    }

    private fun updateLastTimeWithoutFace() {
        lastTimeWithoutFace = System.currentTimeMillis()
    }

    companion object {
        const val MIN_PROBABILITY = 0.7
        const val EULER_ANGLEY_LIMIT = 18F
    }
}