package no.uio.ifi.jonaspr.sensorrecord.recordservice

import android.hardware.Sensor
import android.hardware.SensorEventListener

interface SensorManagerInterface {
    fun getDefaultSensor(type: Int): Sensor
    fun registerListener(
        listener: SensorEventListener,
        sensor: Sensor,
        samplingPeriodUs: Int,
        maxReportLatencyUs: Int
    ): Boolean
    fun unregisterListener(listener: SensorEventListener)
}