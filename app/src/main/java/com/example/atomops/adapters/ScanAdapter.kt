package com.example.atomops.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.activities.BoxDetailsAct
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.databinding.CustomScanBinding
import com.example.atomops.models.DocketBox

data class ScanItem(
    val docketNo: String,
    val boxes: List<ScanBox>
)

data class ScanBox(
    val boxNo: String,
    val isScanned: Boolean
)

class ScanAdapter(val items: List<DocketBox>) : RecyclerView.Adapter<ScanAdapter.ScanViewHolder>() {

    private val expandedPositions = mutableSetOf<String>()

    inner class ScanViewHolder(
        val binding: CustomScanBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScanViewHolder {
        val binding = CustomScanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ScanViewHolder,
        position: Int
    ) {
        val item = items[position]
        val binding = holder.binding

        binding.tvDocketNo.text = item.docketNo
       binding.tvDocketProgress.setText(item.boxesScanned.toString()+"/"+item.boxCountTotal.toString())


       // binding.tvDocketProgress.text = "$scannedCount/$totalCount"

        binding.ivStatus.setImageResource(

                R.drawable.bg_circle_orange

        )


        val isExpanded = item.docketNo in expandedPositions

        binding.llBoxes.visibility =
            if (isExpanded) View.VISIBLE else View.GONE

        binding.ivArrow.rotation = if (isExpanded) 180f else 0f

        binding.llBoxes.removeAllViews()



        binding.headerRow.setOnClickListener {
            /*if (isExpanded) {
                expandedPositions.remove(item.docketNo)
            } else {
                expandedPositions.add(item.docketNo)
            }*/

          //  notifyItemChanged(holder.adapterPosition)
            val context = holder.itemView.context

            val intent = Intent(context, BoxDetailsAct::class.java)
            intent.putExtra("docketno", item.docketNo)
            context.startActivity(intent)


        }
    }

    override fun getItemCount(): Int = items.size
}