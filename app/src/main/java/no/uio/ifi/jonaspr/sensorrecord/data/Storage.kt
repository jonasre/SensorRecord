package no.uio.ifi.jonaspr.sensorrecord.data

import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Storage {
    private const val TAG = "STORAGE"
    private lateinit var path : File
    private val UNITS = arrayOf("B", "kB", "MB", "GB", "TB")
    private val mStatus = MutableLiveData("")
    val status: LiveData<String> = mStatus

    fun initialize(context: Context) {
        path = context.applicationContext.filesDir
    }

    fun createFile(title: String) : File {
        val file = File(path, "TEMP_$title")
        file.createNewFile()
        return file
    }

    fun appendToFile(s: String, f: File) {
        Log.d(TAG, "Appending string with length ${s.length}")
        val writer = FileOutputStream(f, true)
        writer.write(s.toByteArray())
        writer.close()
    }

    fun buildFinalFile(sr: SensorRecording, progressBar: ProgressBar? = null) {
        // Wait until flush is complete
        mStatus.postValue("Getting final samples...")
        while (sr.flushActive) {
            Thread.sleep(100)
            Log.d(TAG, "Waiting for flush to complete")
        }

        mStatus.postValue("Writing headers and markers...")
        Log.d(TAG, "Writing headers and markers")
        // Create writer
        val newFile = File(path, "${sr.title}.txt")
        val writer = FileOutputStream(newFile)
        // Write headers and markers
        writer.write(sr.headerMarkerString().toByteArray())

        mStatus.postValue("Writing sensor data to file...")
        Log.d(TAG, "Writing sensor data to new file")
        // Create reader
        val reader = BufferedReader(FileReader(sr.file))
        // Write all sensor data
        var line = reader.readLine()
        var x = 0
        while (line != null) {
            x++
            writer.write("$line\n".toByteArray())
            line = reader.readLine()
            progressBar?.progress = ((x / sr.samples.toFloat()) * 100).toInt()
        }

        // Close writer and reader
        writer.close()
        reader.close()

        // Delete the temp file
        sr.file.delete()

        // Compress the new file
        mStatus.postValue("Compressing...")
        zipFile(newFile, sr.title)

        // Delete uncompressed file
        newFile.delete()

        Log.d(TAG, "Final file build complete! Found $x lines")
    }

    // from https://www.baeldung.com/java-compress-and-uncompress
    // Creates a zip file
    private fun zipFile(f: File, title: String) {
        Log.d(TAG, "Compressing file")
        val fos = FileOutputStream("$path/$title.zip")
        val zipOut = ZipOutputStream(fos)
        val fis = FileInputStream(f)

        val zipEntry = ZipEntry(f.name)
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipOut.write(bytes, 0, length)
        }
        zipOut.close()
        fis.close()
        fos.close()
    }

    fun getAllZipFiles(): List<File> {
        Log.d(TAG, "Getting all files from directory")
        val parent = File(path.toString())
        val fileList = parent.list()?.filter {
            it.split(".").last() == "zip" && it.split(".").size > 1
        }
        Log.d(TAG, "Found ${fileList?.size} files")

        return fileList?.map { File(parent, it) }!!.sortedDescending()
    }

    fun sizeBytesPrettyString(bytes: Long): String {
        var asDouble = bytes.toDouble()
        var unitIndex = 0
        while (asDouble > 1024) {
            asDouble /= 1024
            unitIndex++
        }
        return "${String.format("%.2f", asDouble)} ${UNITS[unitIndex]}"
    }

    fun checkZipFileExists(title: String): Boolean {
        val t = "${title.lowercase()}.zip"
        for (f in getAllZipFiles()) {
            if (f.name.lowercase() == t) return true
        }
        return false
    }
}