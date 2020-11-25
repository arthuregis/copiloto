package br.pizao.copiloto.service.impl

import android.hardware.Sensor
import android.hardware.SensorEventListener

interface SensorEventListenerImpl : SensorEventListener {

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}