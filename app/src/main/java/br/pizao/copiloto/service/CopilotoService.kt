package br.pizao.copiloto.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Binder
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import br.pizao.copiloto.R
import br.pizao.copiloto.facedetector.FaceDetectorProcessor
import br.pizao.copiloto.manager.CopilotoAudioManager
import br.pizao.copiloto.overlay.GraphicOverlay
import br.pizao.copiloto.utils.CameraHelper
import br.pizao.copiloto.utils.NotificationHelper
import br.pizao.copiloto.utils.Preferences
import br.pizao.copiloto.utils.Preferences.CAMERA_STATUS
import br.pizao.copiloto.utils.Preferences.TTS_ENABLED
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue


class CopilotoService : LifecycleService() {
    private val binder = CameraBinder()

    private lateinit var previewSize: Size
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    private var captureSession: CameraCaptureSession? = null
    private var faceDetector: FaceDetectorProcessor? = null
    private var textureView: TextureView? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var cameraDevice: CameraDevice? = null
    private var tts: TextToSpeech? = null

    private val ttsEnabled = Preferences.booleanLiveData(TTS_ENABLED)
    private var isSpeaking = false
    private var textToSpeech = "null"


    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val mediaImage = reader?.acquireLatestImage()
        mediaImage?.let {
            val orientation = CameraHelper.getRotationCompensation(cameraId, this)
            faceDetector?.processImage(it, orientation) {
                it.close()
            } ?: it.close()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            cameraDevice = currentCameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            currentCameraDevice.close()
            cameraDevice = null
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    private val ttsListener = TextToSpeech.OnInitListener { initStatus ->
        Log.d("DEBUGTTS", "tts OnInitListener")
        if (initStatus == TextToSpeech.SUCCESS) {
            var result = tts?.setLanguage(Locale("pt", "BR"))
            if (result in listOf(
                    TextToSpeech.LANG_MISSING_DATA,
                    TextToSpeech.LANG_NOT_SUPPORTED
                )
            ) {
                result = tts?.setLanguage(Locale("pt", "POR"))
                if (result in listOf(
                        TextToSpeech.LANG_MISSING_DATA,
                        TextToSpeech.LANG_NOT_SUPPORTED
                    )
                ) {
                    Toast.makeText(
                        applicationContext,
                        R.string.tts_pt_not_supported,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                applicationContext,
                R.string.tts_init_failed,
                Toast.LENGTH_LONG
            ).show()
        }
        ttsSpeak()
    }

    private var ttsProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            isSpeaking = true
            CopilotoAudioManager.setVolumetoMax()
        }

        override fun onDone(utteranceId: String?) {
            isSpeaking = false
            CopilotoAudioManager.resetVolume()
        }

        override fun onError(p0: String?) {
            isSpeaking = true
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            isSpeaking = false
            CopilotoAudioManager.resetVolume()
        }

    }


    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            CAMERA_START_ACTION -> {
                faceDetector = FaceDetectorProcessor()
                startCamera()
            }
            TTS_SPEAK_ACTION -> {
                textToSpeech = intent.extras?.get(TTS_TEXT_KEY).toString()
                ttsSpeak()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        ttsEnabled.observe(this) {
            if (it && tts == null) {
                tts = TextToSpeech(this, ttsListener).apply {
                    setOnUtteranceProgressListener(ttsProgressListener)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        faceDetector?.close()
        tts?.stop()
        tts?.shutdown()
        Preferences.putBoolean(CAMERA_STATUS, false)
    }

    fun startPreview(texView: TextureView, graphicOverlay: GraphicOverlay) {
        textureView = texView
        imageWidth = maxOf(imageWidth, texView.width / 3)
        imageHeight = maxOf(imageHeight, texView.height / 3)

        graphicOverlay.setImageSourceInfo(imageWidth, imageHeight, true)
        faceDetector = FaceDetectorProcessor(graphicOverlay)

        if (!texView.isAvailable) {
            texView.surfaceTextureListener = surfaceTextureListener
        } else {
            startCamera()
        }
    }


    @SuppressLint("MissingPermission")
    private fun startCamera() {
        startForeground(CAMERA_CHANEL_ID, NotificationHelper.buildCameraNotification(this))
        Preferences.putBoolean(CAMERA_STATUS, true)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraId = id
                break
            }
        }

        previewSize = chooseSupportedSize()
        cameraManager.openCamera(cameraId, stateCallback, null)
    }

    private fun stopCamera() {
        captureSession?.close()
        imageReader?.close()
        previewSurface?.release()
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun chooseSupportedSize(): Size {

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)

        val texViewArea = imageWidth * imageHeight
        val texViewAspect = imageWidth.toFloat() / imageHeight.toFloat()

        val nearestToFurthestSz = supportedSizes?.sortedWith(compareBy(
            {
                (texViewArea - it.width * it.height).absoluteValue
            },
            {
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat() / it.width.toFloat()
                (aspect - texViewAspect).absoluteValue
            }
        ))


        if (nearestToFurthestSz?.isNotEmpty()!!)
            return nearestToFurthestSz[0]

        return Size(480, 360)
    }

    private fun createCaptureSession() {
        try {
            val targetSurfaces = ArrayList<Surface>()

            val requestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {

                    textureView?.let {
                        it.surfaceTexture?.setDefaultBufferSize(
                            previewSize.width,
                            previewSize.height
                        )
                        previewSurface = Surface(it.surfaceTexture)

                        targetSurfaces.add(previewSurface!!)
                        addTarget(previewSurface!!)
                    }


                    imageReader = ImageReader.newInstance(
                        previewSize.width, previewSize.height,
                        ImageFormat.YUV_420_888, 3
                    )
                    imageReader?.let {
                        it.setOnImageAvailableListener(imageListener, null)
                        targetSurfaces.add(it.surface)
                        addTarget(it.surface)
                    }

                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )
                }


            cameraDevice!!.createCaptureSession(
                targetSurfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (null == cameraDevice) {
                            return
                        }
                        captureSession = cameraCaptureSession
                        try {
                            val captureRequest = requestBuilder.build()
                            captureSession?.setRepeatingRequest(captureRequest, null, null)

                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun ttsSpeak() {
        if (textToSpeech == "null") return
        tts?.speak(textToSpeech, TextToSpeech.QUEUE_ADD, null, "ut")
    }


    inner class CameraBinder : Binder() {
        fun getService(): CopilotoService = this@CopilotoService
    }

    companion object {
        const val CAMERA_CHANEL_ID = 999
        const val CAMERA_START_ACTION = "br.pizao.copiloto.camera.START"
        const val TTS_SPEAK_ACTION = "br.pizao.copiloto.camera.TTS"
        const val TTS_TEXT_KEY = "tts_text_key"

        var imageWidth = 480
        var imageHeight = 360
    }
}
