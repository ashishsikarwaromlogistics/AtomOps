package com.example.atomops.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemRequestBinding
import com.example.atomops.models.dummy.RequestStatus
import com.example.atomops.models.dummy.PickupRequest

class RequestAdapter(
    private val onMenuClick: (PickupRequest) -> Unit = {},
    private val onItemClick: (PickupRequest) -> Unit = {}
) : ListAdapter<PickupRequest, RequestAdapter.VH>(Diff) {

    inner class VH(val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PickupRequest) {
            binding.item = item

            val ctx = binding.root.context
            val (bg, fg) = when (item.status) {
                RequestStatus.CREATED -> R.color.created_bg to R.color.created_text
                RequestStatus.OUT_FOR_PICKUP -> R.color.pickup_bg to R.color.pickup_text
            }
            binding.tvStatus.text = item.status.label
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, fg))
            val bgDrawable = DrawableCompat.wrap(
                ContextCompat.getDrawable(ctx, R.drawable.bg_status_chip)!!.mutate()
            )
            DrawableCompat.setTint(bgDrawable, ContextCompat.getColor(ctx, bg))
            binding.tvStatus.background = bgDrawable

            binding.ivMore.setOnClickListener { onMenuClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<PickupRequest>() {
        override fun areItemsTheSame(a: PickupRequest, b: PickupRequest) = a.id == b.id
        override fun areContentsTheSame(a: PickupRequest, b: PickupRequest) = a == b
    }
}