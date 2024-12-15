package com.qrscanner.fragment.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qrscanner.entity.QrScan
import com.qrscanner.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var scans: List<QrScan>,
    private val onItemClick: (QrScan) -> Unit,
    private val onItemLongClick: (QrScan) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(qrScan: QrScan) {
            binding.contentText.text = qrScan.content
            binding.typeText.text = if (qrScan.isUrl) "URL" else "Text"
            binding.dateText.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(qrScan.scanDateTime)

            // Set click listeners
            itemView.setOnClickListener { onItemClick(qrScan) }
            itemView.setOnLongClickListener {
                onItemLongClick(qrScan)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(scans[position])
    }

    override fun getItemCount() = scans.size

    fun updateScans(newScans: List<QrScan>) {
        scans = newScans
        notifyDataSetChanged()
    }
}