@file:Suppress("DEPRECATION")

package no.uio.ifi.jonaspr.sensorrecord.ui.record

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import no.uio.ifi.jonaspr.sensorrecord.R
import no.uio.ifi.jonaspr.sensorrecord.databinding.FragmentRecordBinding
import no.uio.ifi.jonaspr.sensorrecord.recordservice.RecordService
import java.text.DateFormat


class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private lateinit var recordViewModel: RecordViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var service: RecordService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recordViewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        val root: View = binding.root

        if (isMyServiceRunning(RecordService::class.java)) {
            Log.d(TAG, "Service running, binding...")
            recordViewModel.bindService(context, Intent(context, RecordService::class.java))
        }

        if (recordViewModel.hasSensor(root.context, Sensor.TYPE_PRESSURE)) {
            _binding!!.captureCount.visibility = View.VISIBLE
        }

        _binding!!.startButton.setOnClickListener {
            if (service == null) {
                val title = _binding!!.titleInput.text.toString()
                if (title.isBlank()) {
                    _binding!!.titleInput.error = getString(R.string.set_title)
                    return@setOnClickListener
                }
                val serviceStarted = recordViewModel.startRecordingService(context, title)
                if (!serviceStarted) {
                    val builder: AlertDialog.Builder? = activity?.let { fragment ->
                        AlertDialog.Builder(fragment)
                    }
                    builder?.apply {
                        setTitle("Error: Sensors not found")
                        setMessage(getString(R.string.no_sensor_error))
                        setPositiveButton("Ok") { _, _ -> }
                    }
                    builder?.show()
                } else if (recordViewModel.hasSensor(root.context, Sensor.TYPE_PRESSURE)) {
                    _binding!!.captureCount.visibility = View.VISIBLE
                }
            }
        }

        _binding!!.stopButton.setOnClickListener {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }
            builder?.apply {
                setTitle("Confirmation")
                setMessage("Are you sure you want to stop the recording?")
                setPositiveButton("Yes") { _, _ ->
                    Log.d(TAG, "Dialog OK")
                    stop(recordViewModel)
                    binding.titleInput.setText("")
                    binding.captureCount.visibility = View.INVISIBLE
                }
                setNegativeButton("No") { _, _ ->
                    Log.d(TAG, "Dialog CANCEL")
                }
            }
            builder?.show()
        }

        _binding!!.markerButton.setOnClickListener {
            val dataObject = service?.getDataObject()
            val timestamp = SystemClock.elapsedRealtimeNanos() / 1_000_000
            val timestampReadable = System.currentTimeMillis()

            if (dataObject == null) return@setOnClickListener

            // Create dialog builder
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)
            }

            val dateFormat = DateFormat.getTimeInstance()
            val view = activity?.layoutInflater?.inflate(R.layout.dialog_add_marker, null)

            // Get elements
            val spinner = view?.findViewById<Spinner>(R.id.markerSpinner)!!
            val textField = view.findViewById<EditText>(R.id.markerName)

            // Build dialog
            builder?.setView(view)?.apply {
                setTitle("Add marker at ${dateFormat.format(timestampReadable)}")

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                                pos: Int, id: Long) {
                        textField.visibility =
                            if (pos == spinner.count-1) View.VISIBLE else View.GONE
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                if (service != null) spinner.setSelection(service!!.currentMarkerIndex)

                setPositiveButton("Ok") { _, _ ->
                    Log.d(TAG, "Dialog OK")
                    var name = spinner.selectedItem.toString()

                    // If custom is selected
                    if (spinner.selectedItemPosition == spinner.adapter.count-1) {
                        name = textField.text.toString().trim()
                    } else {
                        service?.currentMarkerIndex = spinner.selectedItemPosition + 1
                    }

                    // Add the marker
                    dataObject.addMarker(name, timestamp)
                    // Notify user of successful creation of marker
                    Toast.makeText(
                        this@RecordFragment.context,
                        "Marker '$name' has been saved",
                        Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("Cancel") { _, _ ->
                    Log.d(TAG, "Dialog CANCEL")
                }

            }
            Log.d(TAG, "Creating dialog, builder: $builder")
            builder?.show()
        }

        recordViewModel.binder().observe(viewLifecycleOwner) { binder ->
            Log.d(TAG, "Binder change, is null: ${binder == null}")
            if (binder != null) {
                service = binder.getService()
                service!!.pressure().observe(viewLifecycleOwner) {
                    val text = "$it hPa"
                    _binding!!.captureCount.text = text
                }

                // Set the title text in case the app has restarted
                _binding!!.titleInput.text = SpannableStringBuilder(service!!.getDataObject().title)

                // Observe service running or not (running = true, stopped = false)
                service!!.running().observe(viewLifecycleOwner) {
                    _binding?.apply {
                        startButton.isVisible = !it
                        stopButton.isVisible = it
                        markerButton.isVisible = it
                        titleInput.isEnabled = !it
                    }
                }
            }
            _binding!!.startButton.isEnabled = true
        }
        return root
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (service != null) {
            recordViewModel.unbindService(context)
            service = null
        }
    }

    private fun stop(viewModel: RecordViewModel) {
        if (service != null) {
            service!!.stop()
            viewModel.unbindService(context)
            service = null
        }
    }

    companion object {
        private const val TAG = "RecordFragment"
    }
}