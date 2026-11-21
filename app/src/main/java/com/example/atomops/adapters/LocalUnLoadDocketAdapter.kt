package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.CustomScanBinding

/** One docket row on the Local Unloading screen (built from getTripUnLoading + local DB). */
data class UnloadDocketRow(
    val docketNo: String,
    val boxIds: List<String>,
    val scannedBoxes: Int          // scanned on this device (LocalLoadbarcode)
) {
    val totalBoxes get() = boxIds.size
    val isComplete get() = totalBoxes > 0 && scannedBoxes >= totalBoxes
}

class LocalUnLoadDocketAdapter(
    private val clickitem: OnItemClickListener,
    private var dockets: List<UnloadDocketRow> = emptyList()
) : RecyclerView.Adapter<LocalUnLoadDocketAdapter.VH>() {

    inner class VH(val b: CustomScanBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = CustomScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = dockets.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = dockets[position]
        val b = holder.b

        b.tvDocketNo.text = item.docketNo
        b.tvDocketProgress.text = "${item.scannedBoxes}/${item.totalBoxes}"

        // status dot: green if every box scanned, orange otherwise
        b.ivStatus.setImageResource(
            if (item.isComplete) R.drawable.dot_green
            else R.drawable.bg_circle_orange
        )

        // row opens the box details screen, so no expand/collapse here
        b.ivArrow.visibility = View.GONE
        b.llBoxes.removeAllViews()
        b.llBoxes.visibility = View.GONE

        b.headerRow.setOnClickListener { clickitem.onItemClick(item.docketNo) }
    }

    fun submitDockets(list: List<UnloadDocketRow>) {
        dockets = list
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(docketNo: String)
    }
}
