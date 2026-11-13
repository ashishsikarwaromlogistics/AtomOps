package com.example.atomops.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemLocalLoadBoxBinding
import com.example.atomops.models.localloadbox.LoadBoxItem

class LocalLoadBoxAdapter(
    private val onActionClick: (LoadBoxItem) -> Unit
) : ListAdapter<LoadBoxItem, LocalLoadBoxAdapter.VH>(DIFF) {

    inner class VH(val b: ItemLocalLoadBoxBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLocalLoadBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b

        b.tvBoxCode.text = item.boxCode
        b.ivIcon.setImageResource(
            if (item.scanned) R.drawable.ic_check_circle_green else R.drawable.ic_box_outline
        )

        // subtitle: show every active flag
        val subtitles = buildList {
            if (item.scanned && !item.scannedAt.isNullOrEmpty()) add("Scanned at ${item.scannedAt}")
            if (item.isDamage)  add("Damage${if (item.damageRemarks.isNotBlank()) " • ${item.damageRemarks}" else ""}")
            if (item.isShort)   add("Short${if (item.shortRemarks.isNotBlank()) " • ${item.shortRemarks}" else ""}")
        }
        if (subtitles.isEmpty()) {
            b.tvSubtitle.visibility = View.GONE
        } else {
            b.tvSubtitle.visibility = View.VISIBLE
            b.tvSubtitle.text = subtitles.joinToString("   ")
            b.tvSubtitle.setTextColor(Color.parseColor("#6B7280"))
        }

        // ONE primary action per tap, priority: undo damage > undo short > mark damage > mark short
        when {
            item.isDamage -> {
                b.tvAction.text = "Undo Damage"
                b.tvAction.setTextColor(Color.parseColor("#E53935"))
            }
            item.isShort -> {
                b.tvAction.text = "Undo Short"
                b.tvAction.setTextColor(Color.parseColor("#E53935"))
            }
            item.scanned -> {
                b.tvAction.text = "Mark Damage"
                b.tvAction.setTextColor(Color.parseColor("#0B3A6F"))
            }
            else -> {
                b.tvAction.text = "Mark Short"
                b.tvAction.setTextColor(Color.parseColor("#0B3A6F"))
            }
        }
        b.tvAction.setOnClickListener { onActionClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LoadBoxItem>() {
            override fun areItemsTheSame(old: LoadBoxItem, new: LoadBoxItem) = old.boxCode == new.boxCode
            override fun areContentsTheSame(old: LoadBoxItem, new: LoadBoxItem) = old == new
        }
    }
}
