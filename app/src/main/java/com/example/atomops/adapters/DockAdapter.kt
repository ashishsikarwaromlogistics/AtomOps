package com.example.atomops.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemDockBinding
import com.example.atomops.models.dummy.Dock
import com.example.atomops.models.dummy.DockDirection
import com.example.atomops.models.dummy.DockSlot

class DockAdapter(
    private val onSlotClick: (Dock, DockSlot) -> Unit = { _, _ -> },
    private val onDockClick: (Dock) -> Unit = {}
) : ListAdapter<Dock, DockAdapter.VH>(Diff) {

    inner class VH(val b: ItemDockBinding) : RecyclerView.ViewHolder(b.root) {

        private var current: Dock? = null
        private val slotAdapter = DockSlotAdapter { slot -> current?.let { onSlotClick(it, slot) } }

        init {
            b.rvSlots.layoutManager = LinearLayoutManager(b.root.context)
            b.rvSlots.adapter = slotAdapter
            b.rvSlots.isNestedScrollingEnabled = false
        }

        fun bind(dock: Dock) {
            current = dock
            b.item = dock
            val ctx = b.root.context
            val density = ctx.resources.displayMetrics.density

            // Direction tag
            b.tvDirection.text = dock.direction.label
            b.tvDirection.setCompoundDrawablesWithIntrinsicBounds(
                0, 0,
                if (dock.direction == DockDirection.OUTBOUND) R.drawable.ic_arrow_out
                else R.drawable.ic_arrow_in,
                0
            )

            // One colored dot per slot
            b.dots.removeAllViews()
            val size = (10 * density).toInt()
            val gap = (8 * density).toInt()
            dock.slots.forEach { slot ->
                val dot = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = gap }
                    setBackgroundResource(R.drawable.bg_dot)
                    backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(ctx, slot.status.dot))
                }
                b.dots.addView(dot)
            }

            // Truck: faded when nothing is docked
            b.ivTruck.alpha = if (dock.hasVehicle) 0.9f else 0.15f

            slotAdapter.submitList(dock.slots)

            b.root.setOnClickListener { onDockClick(dock) }
            b.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDockBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<Dock>() {
        override fun areItemsTheSame(a: Dock, b: Dock) = a.id == b.id
        override fun areContentsTheSame(a: Dock, b: Dock) = a == b
    }
}