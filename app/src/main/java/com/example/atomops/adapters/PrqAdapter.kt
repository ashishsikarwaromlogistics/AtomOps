package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.models.PrqRequest
import com.example.atomops.models.RequestStatus
import com.example.atomops.models.RequestType

class PrqAdapter(
    private val onItemClick: (PrqRequest) -> Unit,
    private val onMoreClick: (View, PrqRequest) -> Unit
) : ListAdapter<PrqRequest, PrqAdapter.PrqViewHolder>(DIFF) {

    inner class PrqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPrqId: TextView = itemView.findViewById(R.id.tvPrqId)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)
        private val tvCustomer: TextView = itemView.findViewById(R.id.tvCustomer)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvSlotLabel: TextView = itemView.findViewById(R.id.tvSlotLabel)
        private val tvPickupSlot: TextView = itemView.findViewById(R.id.tvPickupSlot)
        private val tvLoadType: TextView = itemView.findViewById(R.id.tvLoadType)

        fun bind(item: PrqRequest) {
            val ctx = itemView.context

            tvPrqId.text = item.id
            tvCreatedAt.text = item.createdAt
            tvCustomer.text = item.customer
            tvLocation.text = item.location
            tvPhone.text = item.phone
            tvLoadType.text = item.loadType
            tvPickupSlot.text = "${item.slotDate}  |  ${item.slotTime}"
            tvSlotLabel.text =
                if (item.type == RequestType.PICKUP) "Pickup Slot" else "Drop Slot"

            // Status chip
            tvStatus.text = item.status.label
            when (item.status) {
                RequestStatus.CREATED -> {
                    //tvStatus.setBackgroundResource(R.drawable.bg_status_chip)
                    tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_created_text))
                }
                RequestStatus.OUT_FOR_PICKUP -> {
                  //  tvStatus.setBackgroundResource(R.drawable.bg_status_chip)
                    tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_pickup_text))
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
            ivMore.setOnClickListener { onMoreClick(it, item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrqViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prq, parent, false)
        return PrqViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrqViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PrqRequest>() {
            override fun areItemsTheSame(old: PrqRequest, new: PrqRequest) = old.id == new.id
            override fun areContentsTheSame(old: PrqRequest, new: PrqRequest) = old == new
        }
    }
}
