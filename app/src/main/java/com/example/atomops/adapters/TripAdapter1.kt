package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemTrip1Binding
import com.example.atomops.models.dummy.Trip

class TripAdapter1(
    private val onMenuClick: (Trip) -> Unit = {},
    private val onItemClick: (Trip) -> Unit = {}
) : ListAdapter<Trip, TripAdapter1.VH>(Diff) {

    inner class VH(val b: ItemTrip1Binding) : RecyclerView.ViewHolder(b.root) {
        fun bind(trip: Trip) {
            b.item = trip
            val ctx = b.root.context

            b.tvStatus.text = trip.status.label
            b.tvStatus.setTextColor(ContextCompat.getColor(ctx, trip.status.fg))

            val chip = DrawableCompat.wrap(
                ContextCompat.getDrawable(ctx, R.drawable.bg_status_chip)!!.mutate()
            )
            DrawableCompat.setTint(chip, ContextCompat.getColor(ctx, trip.status.bg))
            b.tvStatus.background = chip

            b.ivMore.setOnClickListener { onMenuClick(trip) }
            b.root.setOnClickListener { onItemClick(trip) }
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTrip1Binding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(a: Trip, b: Trip) = a.id == b.id
        override fun areContentsTheSame(a: Trip, b: Trip) = a == b
    }
}