package no.uio.ifi.jonaspr.sensorrecord.ui.record

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import no.uio.ifi.jonaspr.sensorrecord.recordservice.RecordService

class RecordViewModel : ViewModel() {
    private var mBinder =  MutableLiveData<RecordService.LocalBinder?>()

    fun binder() : MutableLiveData<RecordService.LocalBinder?> {
        return mBinder
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as RecordService.LocalBinder
            mBinder.value = binder
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBinder.value = null
        }

    }

    fun startRecordingService(context: Context?, title: String) : Boolean {
        val accelerometer = hasSensor(context, Sensor.TYPE_ACCELEROMETER)
        val barometer = hasSensor(context, Sensor.TYPE_PRESSURE)
        if (accelerometer || barometer) {
            val intent = Intent(context, RecordService::class.java)
            intent.putExtra("title", title)
            intent.putExtra("accelerometer", accelerometer)
            intent.putExtra("barometer", barometer)
            context?.applicationContext?.startForegroundService(intent)
            bindService(context, intent)
            return true
        }
        return false
    }

    fun bindService(context: Context?, intent: Intent) {
        context?.applicationContext?.bindService(intent, connection, Context.BIND_ABOVE_CLIENT)
    }

    fun unbindService(context: Context?) {
        context?.applicationContext?.unbindService(connection)
        mBinder.value = null
    }

    fun hasSensor(context: Context?, type: Int): Boolean {
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(type)
        return sensor != null
    }

}