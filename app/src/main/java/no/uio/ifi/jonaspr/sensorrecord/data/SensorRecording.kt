package no.uio.ifi.jonaspr.sensorrecord.data

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SensorRecording(val title: String, val delay: Int) {

    var dataString: String = ""
    //val dataList: ArrayList<String> = ArrayList()
    private val markers: ArrayList<Pair<String, Long>> = ArrayList()
    private val startTime: Long = SystemClock.elapsedRealtimeNanos()/1_000_000
    var samples = 0
    val file = Storage.createFile(title)
    var flushActive = false


    fun addData(s: String) {
        samples++
        dataString += s+"\n"

        if (dataString.length > MAX_SIZE_STRING) {
            Log.d(TAG, "String length is now ${dataString.length}, flushing...")
            flush()
        }
        //Log.v(TAG, "String length: ${dataString.length}")
    }

    fun flush() {
        Log.d(TAG, "Flush")
        flushActive = true
        val copy = dataString
        CoroutineScope(Dispatchers.IO).launch {
            Storage.appendToFile(copy, file)
            flushActive = false
        }
        dataString = ""
    }

    fun addMarker(name: String, timestamp: Long) {
        markers.add(Pair(name, timestamp))
    }

    fun headerMarkerString() : String {
        // Add header
        var s = "$title\n$startTime\n\n"

        // Add markers
        for (i in 0 until markers.size) {
            s += "${markers[i].first};${markers[i].second}\n"
        }

        // Extra spacing if there are no markers
        // This is to keep the file structure consistent
        if (markers.size == 0) s += "\n"

        // Spacing
        s += "\n"

        return s
    }
     companion object {
         private const val TAG = "SensorRecording"
         private const val MAX_SIZE_STRING = 40000
     }
}
