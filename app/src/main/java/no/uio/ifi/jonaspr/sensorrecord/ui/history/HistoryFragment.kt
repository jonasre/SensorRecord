package no.uio.ifi.jonaspr.sensorrecord.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import no.uio.ifi.jonaspr.sensorrecord.data.Storage
import no.uio.ifi.jonaspr.sensorrecord.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val allFiles = Storage.getAllZipFiles()
        _binding!!.fileList.adapter = HistoryListAdapter(allFiles)
        _binding!!.fileList
            .addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}