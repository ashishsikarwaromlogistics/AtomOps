package com.example.atomops.adapters




import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.databinding.CustomScanBinding
import com.example.atomops.models.localloadbox.BoxInfo
import com.example.atomops.models.localtripload.TripDocket
import com.example.atomops.R

class LocalLoadDocketAdapter(
    private var clickitem: OnItemClickListener ,
    private var dockets: List<TripDocket> = emptyList(),
    private var boxMap: Map<String, List<BoxInfo>> = emptyMap()
) : RecyclerView.Adapter<LocalLoadDocketAdapter.VH>() {

    private val expanded = mutableSetOf<String>()

    // docketNo -> (scanned, total) from the local DB; updated after every scan
    private var localCounts: Map<String, Pair<Int, Int>> = emptyMap()

    inner class VH(val b: CustomScanBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = CustomScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = dockets.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = dockets[position]
        val docketNo = item.docketNo ?: ""
        val boxes = boxMap[docketNo].orEmpty()
        val b = holder.b

        b.tvDocketNo.text = docketNo

        // local scan progress when we have it, otherwise the server values
        val local = localCounts[docketNo]
        val isComplete: Boolean
        if (local != null) {
            val (scanned, total) = local
            b.tvDocketProgress.text = "$scanned/$total"
            isComplete = total > 0 && scanned >= total
        } else {
            b.tvDocketProgress.text = "${item.loadedBoxes}/${item.totalBoxes}"
            isComplete = item.fullyAccounted == true
        }

        // green tick when every box of the docket is scanned, orange otherwise
        b.ivStatus.setImageResource(
            if (isComplete) R.drawable.green_check
            else R.drawable.bg_circle_orange
        )

        // arrow only if there are boxes to show
        b.ivArrow.visibility = if (boxes.isNotEmpty()) View.VISIBLE else View.GONE

        // child box list
        b.llBoxes.removeAllViews()
        if (expanded.contains(docketNo) && boxes.isNotEmpty()) {
            boxes.forEach { box ->
                val tv = TextView(b.root.context).apply {
                    text = "${box.boxCode}  •  ${box.status ?: "-"}"
                    textSize = 13f
                    setPadding(0, 6, 0, 6)
                }
                b.llBoxes.addView(tv)
            }
            b.llBoxes.visibility = View.VISIBLE
            b.ivArrow.rotation = 180f
        } else {
            b.llBoxes.visibility = View.GONE
            b.ivArrow.rotation = 0f
        }

        b.headerRow.setOnClickListener {
            /* if (boxes.isEmpty()) return@setOnClickListener
             if (!expanded.add(docketNo)) expanded.remove(docketNo)
             notifyItemChanged(holder.adapterPosition)*/
            clickitem.onItemClick(item)
        }
    }

    fun submitDockets(list: List<TripDocket>) {
        dockets = list
        notifyDataSetChanged()
    }

    fun submitBoxes(map: Map<String, List<BoxInfo>>) {
        boxMap = map
        notifyDataSetChanged()
    }

    /** docketNo -> (scanned, total) from LocalLoadbarcode. Call after every scan. */
    fun submitLocalCounts(map: Map<String, Pair<Int, Int>>) {
        localCounts = map
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(docket: TripDocket)
    }

}