package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.ItemTripBinding
import com.example.atomops.models.Trip1
import com.example.atomops.models.trips.Item

class TripAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onMoreClick: (Item, android.view.View) -> Unit
) : ListAdapter<Item, TripAdapter.TripVH>(Diff) {

    inner class TripVH(val b: ItemTripBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(t: Item) = with(b) {
            tvTripId.text = t.tripNo
            tvDate.text = t.createdAt
            tvStatus.text = t.status
            tvRoute.text = "DELHI TO NOIDA"
            tvService.text = t.routeType
            tvDock.text = "dock"
            tvVehicle.text = t.vehicleNo
            root.setOnClickListener { onItemClick(t) }
         //   ivMore.setOnClickListener { onMoreClick(t, it) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TripVH(ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TripVH, position: Int) =
        holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(a: Item, b: Item) = a.tripNo == b.tripNo
        override fun areContentsTheSame(a: Item, b: Item) = a == b
    }
}