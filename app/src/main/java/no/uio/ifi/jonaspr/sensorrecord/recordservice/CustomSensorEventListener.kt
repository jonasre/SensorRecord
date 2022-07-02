package no.uio.ifi.jonaspr.sensorrecord.recordservice

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import androidx.lifecycle.MutableLiveData
import no.uio.ifi.jonaspr.sensorrecord.data.SensorRecording

class CustomSensorEventListener(
    private val dataObject : SensorRecording,
    private val pressure : MutableLiveData<Float>
    ) : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent?) {
        val v = event?.values!!
        val t : Long = event.timestamp/1_000_000 //Nanoseconds to milliseconds

        when (event.sensor?.type) {
            // Pressure data
            Sensor.TYPE_PRESSURE -> {
                dataObject.addData("$t:${v[0]}")
                pressure.postValue(v[0])
                //Log.d(TAG, "SensorEvent PRESSURE: $t@${v[0]}")
            }
            // Accelerometer data
            Sensor.TYPE_ACCELEROMETER -> {
                dataObject.addData("$t:${v[0]}:${v[1]}:${v[2]}")
                //Log.d(TAG, "SensorEvent ACCELEROMETER: $t@${v[0]}:${v[1]}:${v[2]}")
            }
            else -> {
                Log.w(TAG, "SensorEvent UNKNOWN")
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    companion object {
        private const val TAG = "CustomSensorEventListener"
    }
}