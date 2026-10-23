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
import com.example.atomops.databinding.ItemBoxBinding
import com.example.atomops.activities.BoxItem
import com.example.atomops.activities.BoxStatus

class BoxAdapter(
    private val onActionClick: (BoxItem) -> Unit
) : ListAdapter<BoxItem, BoxAdapter.BoxViewHolder>(DIFF) {

    inner class BoxViewHolder(private val b: ItemBoxBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(box: BoxItem) {
            val ctx = b.root.context
            b.tvBoxId.text = box.box

            if (box.isScan) {
                b.ivStatus.setImageResource(R.drawable.green_check)
                b.ivStatus.imageTintList = null // keep green
                b.tvScannedAt.visibility = View.VISIBLE
                b.tvScannedAt.text = "Scanned at ${box.timeScan.orEmpty()}"
            } else {
                b.ivStatus.setImageResource(R.drawable.grey_box)
                b.ivStatus.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_secondary))
                b.tvScannedAt.visibility = View.GONE
            }

            b.tvAction.text = when {
                box.damage == BoxStatus.DAMAGE -> "Undo Damage"
                box.isshort == BoxStatus.SHORT -> "Undo Short"
                box.isScan -> "Mark Damage"
                else -> "Mark Short"
            }

            b.tvAction.setOnClickListener { onActionClick(box) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val binding = ItemBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BoxItem>() {
            override fun areItemsTheSame(old: BoxItem, new: BoxItem) = old.id == new.id
            override fun areContentsTheSame(old: BoxItem, new: BoxItem) = old == new
        }
    }
}
