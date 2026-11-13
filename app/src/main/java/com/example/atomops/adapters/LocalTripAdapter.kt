package com.example.atomops.adapters



import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.LocalItemTripBinding
import com.example.atomops.models.localtrips.Item

class LocalTripAdapter(
    private val onItemClick: (Item) -> Unit = {},
    private val onMoreClick: (Item, android.view.View) -> Unit = { _, _ -> }
) : ListAdapter<Item, LocalTripAdapter.TripVH>(DIFF) {

    inner class TripVH(val binding: LocalItemTripBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.trip = item
            binding.tvStatus.setTextColor(statusColor(item.trip_status))
            binding.root.setOnClickListener { onItemClick(item) }
            binding.ivMore.setOnClickListener { onMoreClick(item, it) }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripVH {
        val binding = LocalItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripVH(binding)
    }

    override fun onBindViewHolder(holder: TripVH, position: Int) = holder.bind(getItem(position))

    private fun statusColor(status: String): Int = when (status.lowercase()) {
        "loading" -> Color.parseColor("#8B5CF6")
        "unloading" -> Color.parseColor("#F59E0B")
        "completed" -> Color.parseColor("#10B981")
        else -> Color.parseColor("#4B5BD6") // Created
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(a: Item, b: Item) = a.trip_no == b.trip_no
            override fun areContentsTheSame(a: Item, b: Item) = a == b
        }
    }
}
