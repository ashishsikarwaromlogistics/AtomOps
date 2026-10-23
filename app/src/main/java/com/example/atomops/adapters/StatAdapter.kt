package com.example.atomops.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemStatBinding
import com.example.atomops.models.dummy.TripStat
import com.example.atomops.models.dummy.TripStatus

class StatAdapter(
    private val onClick: (TripStat) -> Unit
) : ListAdapter<TripStat, StatAdapter.VH>(Diff) {

    /** null = "Total Trips" is selected */
    var selected: TripStatus? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(val b: ItemStatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(stat: TripStat) {
            b.stat = stat
            val ctx = b.root.context

            val status = stat.status
            if (status == null) {
                b.dot.visibility = View.GONE
            } else {
                b.dot.visibility = View.VISIBLE
                b.dot.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, status.dot))
            }

            val isSelected = stat.status == selected
            val density = ctx.resources.displayMetrics.density
            b.card.strokeColor =
                ContextCompat.getColor(ctx, if (isSelected) R.color.navy else R.color.divider)
            b.card.strokeWidth = ((if (isSelected) 2 else 1) * density).toInt()

            b.root.setOnClickListener { onClick(stat) }
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<TripStat>() {
        override fun areItemsTheSame(a: TripStat, b: TripStat) = a.label == b.label
        override fun areContentsTheSame(a: TripStat, b: TripStat) = a == b
    }
}