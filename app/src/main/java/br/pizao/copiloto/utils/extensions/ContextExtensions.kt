package br.pizao.copiloto.utils.extensions

import android.app.ActivityManager
import android.content.Context
import br.pizao.copiloto.service.CopilotoService

@Suppress("DEPRECATION")
fun Context.isCameraServiceRunning(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (service.service.className == CopilotoService::class.java.name) {
            return true
        }
    }
    return false
}