package no.uio.ifi.jonaspr.sensorrecord.ui.record

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import no.uio.ifi.jonaspr.sensorrecord.recordservice.RecordService

class RecordViewModel : ViewModel() {
    private var mBinder =  MutableLiveData<RecordService.LocalBinder?>()
    private var mTestFileUri = MutableLiveData<Uri?>()

    fun binder() : MutableLiveData<RecordService.LocalBinder?> {
        return mBinder
    }

    fun testFileUri() : MutableLiveData<Uri?> {
        return mTestFileUri
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

    // Verifies that sensors are available, starts the recording service
    // Returns true if service started, false if not (sensors unavailable)
    fun startRecordingService(context: Context?, title: String) : Boolean {
        val accelerometer = hasSensor(context, Sensor.TYPE_ACCELEROMETER)
        val barometer = hasSensor(context, Sensor.TYPE_PRESSURE)

        // If accelerometer or barometer is available
        if (accelerometer || barometer) {
            val intent = Intent(context, RecordService::class.java).apply {
                putExtra("title", title)
                putExtra("accelerometer", accelerometer)
                putExtra("barometer", barometer)
                putExtra("testFileUri", mTestFileUri.value)
            }

            // Start the recording service
            context?.applicationContext?.startForegroundService(intent)

            // Bind the service
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