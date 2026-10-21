package com.example.atomops.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.databinding.CustomScanBinding
import com.example.atomops.databinding.ItemBoxRowBinding

data class DocketItem(
    val docket: TripDetailsTable,
    val scannedBoxes: MutableList<BarcodeTable>,
    var expanded: Boolean = false
) {
    val scannedCount get() = scannedBoxes.count { it.isScan }
    val isComplete get() = scannedCount >= docket.boxes
}

enum class DocketFilter { ALL, PENDING, COMPLETE }

enum class ScanResult { OK, DUPLICATE, NOT_FOUND }

class LoadingAdap(
    private val onDeleteBox: (box: String, tripId: String) -> Unit,
    private val onUpdateBox: (box: String) -> Unit
) : RecyclerView.Adapter<LoadingAdap.DocktVH>() {

    private val allItems = mutableListOf<DocketItem>()   // source of truth (ordered)
    private val shown = mutableListOf<DocketItem>()      // after filter
    private var filter = DocketFilter.ALL
    private var query = ""

    inner class DocktVH(val b: CustomScanBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: DocketItem) = with(b) {
            val total = item.docket.boxes

            tvDocketNo.text = item.docket.docketNo
            tvDocketProgress.text = "${item.scannedCount}/$total"

            if (item.isComplete) ivStatus.setColorFilter(Color.parseColor("#2E7D32"))
            else ivStatus.clearColorFilter()

            llBoxes.removeAllViews()
            val inflater = LayoutInflater.from(root.context)
            if (item.scannedBoxes.isEmpty()) {
                llBoxes.addView(TextView(root.context).apply {
                    text = "No boxes"
                    alpha = 0.6f
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                })
            } else {
                item.scannedBoxes.forEach { box ->
                    val row = ItemBoxRowBinding.inflate(inflater, llBoxes, false)
                    row.tvBox.text = box.box
                    row.llBoxes.setBackgroundColor(
                        Color.parseColor(if (box.isScan) "#00FF00" else "#FF0000") // green = scanned, red = pending
                    )
                    row.ivEdit.setOnClickListener { onUpdateBox(box.box) }
                    row.ivDelete.setOnClickListener { onDeleteBox(box.box, box.tripId) }
                    llBoxes.addView(row.root)
                }
            }

            llBoxes.visibility = if (item.expanded) View.VISIBLE else View.GONE
            ivArrow.rotation = if (item.expanded) 180f else 0f

            headerRow.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                shown[pos].expanded = !shown[pos].expanded
                notifyItemChanged(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DocktVH(CustomScanBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DocktVH, position: Int) = holder.bind(shown[position])

    override fun getItemCount() = shown.size

    // ---------- data ----------

    fun submit(newItems: List<DocketItem>) {
        val expandedNos = allItems.filter { it.expanded }.map { it.docket.docketNo }.toSet()
        newItems.forEach { if (it.docket.docketNo in expandedNos) it.expanded = true }
        allItems.clear()
        allItems.addAll(newItems)
        applyFilter()
    }

    /** Mark a box scanned and MOVE it (and its docket) to the top. */
    fun markScanned(boxValue: String, time: String): ScanResult {
        val docketNo = boxValue.dropLast(4)
        val docket = allItems.firstOrNull { it.docket.docketNo == docketNo }
            ?: return ScanResult.NOT_FOUND
        val idx = docket.scannedBoxes.indexOfFirst { it.box == boxValue }
        if (idx == -1) return ScanResult.NOT_FOUND
        if (docket.scannedBoxes[idx].isScan) return ScanResult.DUPLICATE

        val updated = docket.scannedBoxes[idx].copy(isScan = true, timeScan = time)
        docket.scannedBoxes.removeAt(idx)
        docket.scannedBoxes.add(0, updated)

        allItems.remove(docket)
        allItems.add(0, docket)
        docket.expanded = true
        applyFilter()
        return ScanResult.OK
    }

    /** Un-scan a box; scanned boxes stay above pending ones. */
    fun markUnscanned(boxValue: String) {
        val docket = allItems.firstOrNull { it.docket.docketNo == boxValue.dropLast(4) } ?: return
        val idx = docket.scannedBoxes.indexOfFirst { it.box == boxValue }
        if (idx == -1) return
        docket.scannedBoxes[idx] = docket.scannedBoxes[idx].copy(isScan = false, timeScan = "")
        docket.scannedBoxes.sortByDescending { it.status } // stable sort
        applyFilter()
    }

    // ---------- filter ----------

    fun setFilter(f: DocketFilter) { filter = f; applyFilter() }
    fun setQuery(q: String) { query = q.trim(); applyFilter() }

    private fun applyFilter() {
        shown.clear()
        shown.addAll(allItems.filter {
            val byState = when (filter) {
                DocketFilter.ALL -> true
                DocketFilter.PENDING -> !it.isComplete
                DocketFilter.COMPLETE -> it.isComplete
            }
            val byQuery = query.isEmpty() || it.docket.docketNo.contains(query, ignoreCase = true)
            byState && byQuery
        })
        notifyDataSetChanged()
    }
}