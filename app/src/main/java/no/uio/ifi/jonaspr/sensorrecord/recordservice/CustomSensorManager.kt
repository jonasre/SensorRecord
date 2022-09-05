package no.uio.ifi.jonaspr.sensorrecord.recordservice

import android.hardware.Sensor
import android.hardware.SensorEventListener

class CustomSensorManager: SensorManagerInterface {
    override fun getDefaultSensor(type: Int): Sensor {
        TODO("Not yet implemented")
    }

    override fun registerListener(
        listener: SensorEventListener,
        sensor: Sensor,
        samplingPeriodUs: Int,
        maxReportLatencyUs: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun unregisterListener(listener: SensorEventListener) {
        TODO("Not yet implemented")
    }
}