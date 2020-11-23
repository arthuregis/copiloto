package br.pizao.copiloto.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.pizao.copiloto.R
import br.pizao.copiloto.activities.MainActivity

object NotificationHelper {

    const val CAMERA_CHANEL_ID = "camera_service"
    const val CAMERA_CHANEL_NAME = "camera_copiloto"

    fun buildCameraNotification(context: Context): Notification {
        val pendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(context, 0, notificationIntent, 0)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CAMERA_CHANEL_ID, CAMERA_CHANEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, CAMERA_CHANEL_ID)
            .setContentTitle(context.getText(R.string.app_name))
            .setContentText(context.getText(R.string.app_name))
            .setSmallIcon(R.drawable.camera_notification)
            .setContentIntent(pendingIntent)
            .setTicker(context.getText(R.string.app_name))
            .build()
    }
}