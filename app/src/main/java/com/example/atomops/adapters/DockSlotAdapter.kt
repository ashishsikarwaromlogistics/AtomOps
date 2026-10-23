package com.example.atomops.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.ItemDockSlotBinding
import com.example.atomops.models.dummy.DockSlot

class DockSlotAdapter(
    private val onClick: (DockSlot) -> Unit = {}
) : ListAdapter<DockSlot, DockSlotAdapter.VH>(Diff) {

    inner class VH(val b: ItemDockSlotBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(slot: DockSlot) {
            b.slot = slot
            val ctx = b.root.context

            b.tvBadge.text = slot.status.label
            TextViewCompat.setCompoundDrawableTintList(
                b.tvBadge,
                ColorStateList.valueOf(ContextCompat.getColor(ctx, slot.status.dot))
            )
            b.progress.setProgressCompat(slot.progress, false)

            b.root.setOnClickListener { onClick(slot) }
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDockSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<DockSlot>() {
        override fun areItemsTheSame(a: DockSlot, b: DockSlot) = a.name == b.name
        override fun areContentsTheSame(a: DockSlot, b: DockSlot) = a == b
    }
}