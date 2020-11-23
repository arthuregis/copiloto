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
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.LifecycleService
import br.pizao.copiloto.facedetector.FaceDetectorProcessor
import br.pizao.copiloto.overlay.GraphicOverlay
import br.pizao.copiloto.utils.CameraHelper
import br.pizao.copiloto.utils.NotificationHelper
import br.pizao.copiloto.utils.Preferences
import br.pizao.copiloto.utils.Preferences.CAMERA_STATUS
import kotlin.math.absoluteValue


class CameraService : LifecycleService() {
    private val binder = CameraBinder()

    private lateinit var previewSize: Size
    private lateinit var cameraManager: CameraManager
    private lateinit var imageReader: ImageReader
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var cameraId: String

    private var faceDetector: FaceDetectorProcessor? =null
    private var textureView: TextureView? = null
    private var previewSurface: Surface? = null
    private var cameraDevice: CameraDevice? = null

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
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(CAMERA_CHANEL_ID, NotificationHelper.buildCameraNotification(this))
        Preferences.putBoolean(CAMERA_STATUS, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        faceDetector?.close()
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
        captureSession.close()
        imageReader.close()
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
                    imageReader.setOnImageAvailableListener(imageListener, null)

                    targetSurfaces.add(imageReader.surface)
                    addTarget(imageReader.surface)


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
                            captureSession.setRepeatingRequest(captureRequest, null, null)

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

    inner class CameraBinder : Binder() {
        fun getService(): CameraService = this@CameraService
    }

    companion object {
        const val CAMERA_CHANEL_ID = 999

        const val CAMERA_START_ACTION = "br.pizao.copiloto.camera.START"

        var imageWidth = 480
        var imageHeight = 360
    }
}
