package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemGateTripBinding
import com.example.atomops.models.dummy.GateMode
import com.example.atomops.models.dummy.GateTrip

class GateTripAdapter(
    private val onActionClick: (GateTrip) -> Unit = {},
    private val onMenuClick: (GateTrip) -> Unit = {},
    private val onItemClick: (GateTrip) -> Unit = {}
) : ListAdapter<GateTrip, GateTripAdapter.VH>(Diff) {

    inner class VH(val b: ItemGateTripBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(trip: GateTrip) {
            b.item = trip
            val ctx = b.root.context

            // Status chip
            b.tvStatus.text = trip.status.label
            b.tvStatus.setTextColor(ContextCompat.getColor(ctx, trip.status.fg))
            val chip = DrawableCompat.wrap(
                ContextCompat.getDrawable(ctx, R.drawable.bg_status_chip)!!.mutate()
            )
            DrawableCompat.setTint(chip, ContextCompat.getColor(ctx, trip.status.bg))
            b.tvStatus.background = chip

            // Action button: only for upcoming trips
            b.btnAction.visibility = if (trip.isDone) View.GONE else View.VISIBLE
            b.btnAction.text =
                if (trip.mode == GateMode.GATE_IN) "Mark Gate-In" else "Mark Gate-Out"

            b.btnAction.setOnClickListener { onActionClick(trip) }
            b.ivMore.setOnClickListener { onMenuClick(trip) }
            b.root.setOnClickListener { onItemClick(trip) }
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemGateTripBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<GateTrip>() {
        override fun areItemsTheSame(a: GateTrip, b: GateTrip) = a.id == b.id && a.mode == b.mode
        override fun areContentsTheSame(a: GateTrip, b: GateTrip) = a == b
    }
}