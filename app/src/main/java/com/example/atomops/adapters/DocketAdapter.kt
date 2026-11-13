package com.example.atomops

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.CustomDocketBinding
import com.example.atomops.models.offlinedata.Docket

class DocketAdapter(
    private val onItemClick: (Docket) -> Unit = {},
    private val onMoreClick: (Docket, android.view.View) -> Unit = { _, _ -> }
) : ListAdapter<Docket, DocketAdapter.DocketViewHolder>(DIFF) {

    inner class DocketViewHolder(val binding: CustomDocketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Docket) {
            binding.item = item
            binding.root.setOnClickListener { onItemClick(item) }
            binding.ivMore.setOnClickListener { onMoreClick(item, it) }
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocketViewHolder {
        val binding = CustomDocketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Docket>() {
            override fun areItemsTheSame(old: Docket, new: Docket) = old.docketNo == new.docketNo
            override fun areContentsTheSame(old: Docket, new: Docket) = old == new
        }
    }
}
