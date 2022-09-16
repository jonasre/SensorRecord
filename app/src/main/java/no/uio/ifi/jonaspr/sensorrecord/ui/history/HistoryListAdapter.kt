package no.uio.ifi.jonaspr.sensorrecord.ui.history

import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import no.uio.ifi.jonaspr.sensorrecord.data.Storage
import no.uio.ifi.jonaspr.sensorrecord.databinding.HistoryItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryListAdapter(var fileList: List<File>) :
    RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    inner class ViewHolder(binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val titleText = binding.titleText
        private val dateText = binding.date
        private val size = binding.size
        private val context = binding.root.context
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy")

        fun bind(file: File) {
            titleText.text = file.name
            dateText.text = dateFormat.format(Date(file.lastModified()))
            size.text = Storage.sizeBytesPrettyString(file.length())

            itemView.setOnClickListener {
                Log.d(TAG, "click item")
                val intentShareFile = Intent(Intent.ACTION_SEND)

                if (file.exists()) {
                    intentShareFile.apply {
                        type = "application/pdf"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(Intent.EXTRA_SUBJECT, "Sharing File from SensorRecord")
                        putExtra(Intent.EXTRA_TEXT, "Description")
                        val fileURI = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".provider",
                            file
                        )
                        putExtra(Intent.EXTRA_STREAM, fileURI)
                    }
                    context.startActivity(intentShareFile)
                } else {
                    Log.w(TAG, "Tried sharing nonexistent file")
                }
            }

            itemView.setOnLongClickListener {
                AlertDialog.Builder(context).apply {
                    setTitle("Confirm delete")
                    setMessage("Are you sure you want to delete '${file.name}'? This action can not be undone.")
                    setPositiveButton("Delete") { _, _ ->
                        if (file.exists()) {
                            if (!file.delete()) {
                                Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()

                            }
                            else {
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                        fileList = Storage.getAllZipFiles()
                        this@HistoryListAdapter.notifyDataSetChanged()
                    }
                    setNegativeButton("Cancel") { _, _ ->
                        Log.d(TAG, "Aborted deletion")
                    }
                }.show()
                return@setOnLongClickListener true
            }
        }

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item


        return ViewHolder(
            HistoryItemBinding.inflate(
                LayoutInflater.from(viewGroup.context),
                viewGroup,
                false
            )
        )
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.bind(fileList[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = fileList.size

    companion object {
        private const val TAG = "HistoryListAdapter"
    }

}
