package com.example.atomops.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.ItemDockStatBinding
import com.example.atomops.models.dummy.DockStat

class DockStatAdapter : ListAdapter<DockStat, DockStatAdapter.VH>(Diff) {

    inner class VH(val b: ItemDockStatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(stat: DockStat) {
            b.stat = stat
            b.dot.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(b.root.context, stat.dotColor))
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDockStatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<DockStat>() {
        override fun areItemsTheSame(a: DockStat, b: DockStat) = a.label == b.label
        override fun areContentsTheSame(a: DockStat, b: DockStat) = a == b
    }
}