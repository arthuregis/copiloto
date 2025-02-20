package br.pizao.copiloto.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.CountDownTimer
import android.text.format.DateUtils
import androidx.lifecycle.LifecycleService
import br.pizao.copiloto.service.impl.SensorEventListenerImpl
import br.pizao.copiloto.utils.Constants.SENSOR_ENABLED
import br.pizao.copiloto.utils.persistence.Preferences

class CopilotoSkin(context: Context, listener: ProximityListener) : SensorEventListenerImpl {

    private val proximityTimer = object : CountDownTimer(2 * DateUtils.SECOND_IN_MILLIS, 1000) {
        override fun onTick(p0: Long) {}

        override fun onFinish() {
            listener.onToClose()
        }

    }

    init {
        val sensorManager =
            context.getSystemService(LifecycleService.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        sensorManager.registerListener(
            this,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (Preferences.getBoolean(SENSOR_ENABLED, defValue = true)) {
            if (p0?.values?.first()?.toInt() == 0) {
                proximityTimer.start()
            } else {
                proximityTimer.cancel()
            }
        }
    }

    interface ProximityListener {
        fun onToClose()
    }
}