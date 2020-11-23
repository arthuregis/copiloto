package br.pizao.copiloto.facedetector

import android.graphics.PointF
import android.media.Image
import android.text.format.DateUtils
import br.pizao.copiloto.manager.CopilotoAudioManager
import br.pizao.copiloto.overlay.FaceGraphic
import br.pizao.copiloto.overlay.GraphicOverlay
import br.pizao.copiloto.service.CopilotoService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorProcessor(private val graphicOverlay: GraphicOverlay? = null) {

    private var isProcessing = false
    private var lastTimeEyeOpen = System.currentTimeMillis()

    private val detector = FaceDetectorOptions.Builder()
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build().let { faceDetectorOptions ->
            FaceDetection.getClient(faceDetectorOptions)
        }

    fun processImage(image: Image, orientation: Int, callback: Runnable) {
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
                        } else {
                            lastTimeEyeOpen = System.currentTimeMillis()
                            //TODO - avisar motorista perda de contato visual
                        }
                        graphicOverlay?.postInvalidate()
                    }
                    .addOnCompleteListener {
                        callback.run()
                        isProcessing = false
                    }
            }
        } else {
            callback.run()
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
            CopilotoAudioManager.horn()
        }
    }

    private fun updateLastTimeEyeOpen() {
        lastTimeEyeOpen = System.currentTimeMillis()
    }

    companion object {
        const val MIN_PROBABILITY = 0.75
        const val EULER_ANGLEY_LIMIT = 18F
    }
}