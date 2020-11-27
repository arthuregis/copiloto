package br.pizao.copiloto.utils.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.pizao.copiloto.utils.Constants.PERMISSION_REQUEST_CODE

class Permissions(private val activity: Activity) {

    private val notGrantedPermissions = arrayListOf<String>()

    fun isNotAllGranted(): Boolean {
        REQUIRED_PERMISSION.forEach {
            if (!isGranted(it)) {
                notGrantedPermissions.add(it)
            }
        }
        return notGrantedPermissions.isNotEmpty()
    }

    fun requestMissing() {
        ActivityCompat.requestPermissions(
            activity,
            notGrantedPermissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }

    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private val REQUIRED_PERMISSION =
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
    }
}