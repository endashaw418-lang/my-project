package com.smartvoice.assistant.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartvoice.assistant.R
import com.smartvoice.assistant.data.local.CommandHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying command history.
 * Uses DiffUtil for efficient list updates.
 */
class HistoryAdapter : ListAdapter<CommandHistoryEntity, HistoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCommand: TextView = view.findViewById(R.id.tvHistoryCommand)
        val tvResponse: TextView = view.findViewById(R.id.tvHistoryResponse)
        val tvTimestamp: TextView = view.findViewById(R.id.tvHistoryTimestamp)
        val tvLanguage: TextView = view.findViewById(R.id.tvHistoryLanguage)
        val ivStatus: ImageView = view.findViewById(R.id.ivHistoryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvCommand.text = item.rawText
        holder.tvResponse.text = item.responseMessage
        holder.tvTimestamp.text = formatTimestamp(item.timestamp)
        holder.tvLanguage.text = item.language

        holder.ivStatus.setImageResource(
            if (item.success) R.drawable.ic_check_circle
            else R.drawable.ic_error
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    class DiffCallback : DiffUtil.ItemCallback<CommandHistoryEntity>() {
        override fun areItemsTheSame(oldItem: CommandHistoryEntity, newItem: CommandHistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommandHistoryEntity, newItem: CommandHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
