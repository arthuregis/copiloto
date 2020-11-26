package br.pizao.copiloto.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import android.view.TextureView
import br.pizao.copiloto.service.impl.SurfaceTextureListenerImpl
import br.pizao.copiloto.ui.overlay.GraphicOverlay
import br.pizao.copiloto.utils.Constants
import br.pizao.copiloto.utils.helpers.CameraHelper
import br.pizao.copiloto.utils.persistence.Preferences
import kotlin.math.absoluteValue

class CopilotoEyes(private val context: Context) :
    CameraDevice.StateCallback(),
    ImageReader.OnImageAvailableListener, SurfaceTextureListenerImpl {

    private lateinit var cameraId: String
    private lateinit var previewSize: Size
    private lateinit var cameraManager: CameraManager

    private var captureSession: CameraCaptureSession? = null
    private var faceDetector: FaceDetectorProcessor? = null
    private var textureView: TextureView? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var cameraDevice: CameraDevice? = null


    fun init(
        listener: CopilotoMouth.SpeechRequester,
        texView: TextureView? = null,
        graphicOverlay: GraphicOverlay? = null
    ) {
        textureView = texView
        texView?.let {
            CopilotoService.imageWidth = maxOf(CopilotoService.imageWidth, it.width / 3)
            CopilotoService.imageHeight = maxOf(CopilotoService.imageHeight, it.height / 3)
        }

        graphicOverlay?.setImageSourceInfo(
            CopilotoService.imageWidth,
            CopilotoService.imageHeight, true
        )

        faceDetector = FaceDetectorProcessor(listener, graphicOverlay)
        if (texView?.isAvailable == false) {
            texView.surfaceTextureListener = this
        } else {
            starWatch()
        }
    }

    override fun onImageAvailable(reader: ImageReader?) {
        val mediaImage = reader?.acquireLatestImage()
        mediaImage?.let {
            val orientation = CameraHelper.getRotationCompensation(cameraId, context)
            faceDetector?.processImage(it, orientation) {
                it.close()
            } ?: it.close()
        }
    }

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

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        starWatch()
    }

    @SuppressLint("MissingPermission")
    fun starWatch() {
        Preferences.putBoolean(Constants.CAMERA_STATUS, true)
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraId = id
                break
            }
        }

        previewSize = chooseSupportedSize()
        cameraManager.openCamera(cameraId, this, null)
    }

    fun stopWatching() {
        captureSession?.close()
        imageReader?.close()
        previewSurface?.release()
        cameraDevice?.close()
        cameraDevice = null
        textureView = null
        faceDetector?.close()
        Preferences.putBoolean(Constants.CAMERA_STATUS, false)
        Preferences.putBoolean(Constants.CAMERA_ON_BACKGROUND, false)
    }

    private fun chooseSupportedSize(): Size {

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)

        val texViewArea = CopilotoService.imageWidth * CopilotoService.imageHeight
        val texViewAspect =
            CopilotoService.imageWidth.toFloat() / CopilotoService.imageHeight.toFloat()

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
                        it.setOnImageAvailableListener(this@CopilotoEyes, null)
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
}