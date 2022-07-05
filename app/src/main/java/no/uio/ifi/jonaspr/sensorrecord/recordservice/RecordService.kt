package no.uio.ifi.jonaspr.sensorrecord.recordservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.uio.ifi.jonaspr.sensorrecord.*
import no.uio.ifi.jonaspr.sensorrecord.data.SensorRecording
import no.uio.ifi.jonaspr.sensorrecord.data.Storage

class RecordService : Service() {
    inner class LocalBinder : Binder() {
        fun getService(): RecordService = this@RecordService
    }

    private val delay = 1_000
    private val barometerSamplingFrequencyHz = 4
    private val accelerometerSamplingFrequencyHz = 60
    private val barometerSamplingFrequency = Util.hzToMicroseconds(barometerSamplingFrequencyHz)
    private val accelerometerSamplingFrequency = Util.hzToMicroseconds(accelerometerSamplingFrequencyHz)
    private val binder = LocalBinder()


    private lateinit var job : Job
    private lateinit var sensorManager : SensorManager
    private lateinit var listener : CustomSensorEventListener
    private lateinit var dataObject : SensorRecording
    private lateinit var alarmManager : AlarmManager
    private lateinit var pendingIntent : PendingIntent
    private lateinit var wakeLock : PowerManager.WakeLock

    private var useAccelerometer: Boolean = true
    var useBarometer: Boolean = true


    private val _running = MutableLiveData(false)
    private val _pressure = MutableLiveData(0F)
    fun running(): LiveData<Boolean> = _running
    fun pressure(): LiveData<Float> = _pressure

    var currentMarkerIndex = 0


    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "RecordService starting")
        val title = intent?.getStringExtra("title") as String
        useAccelerometer = intent.getBooleanExtra("accelerometer", true)
        useBarometer = intent.getBooleanExtra("barometer", true)
        dataObject = SensorRecording(title, delay)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        job = CoroutineScope(Dispatchers.Default).launch {
            // Set up sensor listening
            listener = CustomSensorEventListener(dataObject, _pressure)

            if (useBarometer) {
                // Register listener for barometer
                val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
                var maxReportLatency = (pressureSensor.fifoMaxEventCount/barometerSamplingFrequencyHz)*1_000_000
                if (maxReportLatency < 0) maxReportLatency = Int.MAX_VALUE //integer overflow protection
                val barometerBatching = sensorManager.registerListener(
                    listener,
                    pressureSensor,
                    barometerSamplingFrequency,
                    maxReportLatency
                )
                Log.d(TAG, "MaxReportLatency for barometer: $maxReportLatency")
                Log.d(TAG, "Barometer is wakeupSensor: ${pressureSensor.isWakeUpSensor}")
                // Log if batching isn't available
                if (!barometerBatching)
                    Log.w(TAG, "Batching for barometer not available")
            }

            if (useAccelerometer) {
                // Register listener for accelerometer
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                var maxReportLatency = (accelerometer.fifoMaxEventCount/accelerometerSamplingFrequencyHz)*1_000_000
                if (maxReportLatency < 0) maxReportLatency = Int.MAX_VALUE //integer overflow protection
                val accelerometerBatching = sensorManager.registerListener(
                    listener,
                    accelerometer,
                    accelerometerSamplingFrequency,
                    maxReportLatency
                )
                Log.d(TAG, "MaxReportLatency for accelerometer: $maxReportLatency")
                Log.d(TAG, "Accelerometer is wakeupSensor: ${accelerometer.isWakeUpSensor}")
                // Log if batching isn't available
                if (!accelerometerBatching)
                    Log.w(TAG, "Batching for accelerometer not available")
            }


            // Start foreground service
            _running.postValue(true)
            startForeground()
        }


        val broadcastIntent = Intent(this, MyBroadcastReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            RTC_WAKEUP,
            System.currentTimeMillis(),
            60_000,
            pendingIntent
        )

        // Used to minimize gaps in sensor data
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorRecord::RecordServiceWakeLock").apply {
                acquire()
            }
        }



        return START_REDELIVER_INTENT
    }

    private fun startForeground() {
        val notificationChannelID = BuildConfig.APPLICATION_ID
        val channelName = "RecordService"
        val channel = NotificationChannel(
            notificationChannelID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationID = 1
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }


        val notification: Notification = Notification.Builder(this, notificationChannelID)
            .setOngoing(true)
            .setContentTitle("SensorRecord is running")
            .setContentText("Recording data in the background")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_baseline_fiber_manual_record_24)
            .build()

        startForeground(notificationID, notification)
    }

    fun stop() {
        Log.d(TAG, "Stop signal received")
        _running.postValue(false)
        sensorManager.unregisterListener(listener)
        job.cancel()
        alarmManager.cancel(pendingIntent)
        wakeLock.release()
        stopSelf()

        dataObject.flush()

        // Save recording to file
        CoroutineScope(Dispatchers.IO).launch {
            Storage.buildFinalFile(dataObject)
        }

    }

    override fun onDestroy() {
        Log.d(TAG, "Destroying service")
        if (_running.value == true) {
            // Service was not stopped manually
            stop()
        }
        super.onDestroy()
    }

    fun getDataObject(): SensorRecording {
        return dataObject
    }

    companion object {
        private const val TAG = "recordService"
    }

}