package no.uio.ifi.jonaspr.sensorrecord

object Util {
    // Converts Hz to microseconds
    fun hzToMicroseconds(f : Int) : Int {
        return 1_000_000/f
    }
}