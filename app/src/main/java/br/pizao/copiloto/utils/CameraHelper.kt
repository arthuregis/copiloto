package br.pizao.copiloto.utils

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SparseIntArray
import android.view.Surface
import android.view.WindowManager

object CameraHelper {

    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }

    fun getRotationCompensation(cameraId: String, context: Context): Int {
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val deviceRotation = windowManager.defaultDisplay.rotation
        val rotationCompensation = ORIENTATIONS.get(deviceRotation)

        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        return (sensorOrientation + rotationCompensation ) % 360
    }

}