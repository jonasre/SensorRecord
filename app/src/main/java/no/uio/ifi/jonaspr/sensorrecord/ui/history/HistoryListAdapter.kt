package no.uio.ifi.jonaspr.sensorrecord.ui.history

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import no.uio.ifi.jonaspr.sensorrecord.databinding.HistoryItemBinding
import java.io.File
import java.time.LocalDateTime
import java.util.*

class HistoryListAdapter(private val fileList: Array<String>) :
    RecyclerView.Adapter<HistoryListAdapter.ViewHolder>() {
    private val TAG = "HistoryListAdapter"

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    inner class ViewHolder(binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val titleText = binding.titleText
        private val dateText = binding.date
        private val capturesText = binding.captures
        private val context = binding.root.context

        fun bind(filename: String) {
            val split = filename.split("#")
            if (split.size >= 3) {
                capturesText.text = split[split.lastIndex]
                dateText.text = Date(split[split.lastIndex-1].toLong()).toString()
                var txt = ""
                for (i in 0 until split.size-2) {
                    txt += split[i]
                }
                titleText.text = txt
            } else {
                titleText.text = filename
            }

            itemView.setOnClickListener {
                Log.d(TAG, "click item")
                val intentShareFile = Intent(Intent.ACTION_SEND)
                val filepath = context.applicationContext.filesDir.toString()+"/"+filename
                val file = File(filepath)

                if (file.exists()) {
                    Log.d(TAG, "File exists")
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
                    Log.d(TAG, "File in $filepath doesn't exist")
                }
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

}
