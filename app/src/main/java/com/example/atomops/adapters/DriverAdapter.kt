package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.models.tripdetails.Driver
import com.example.atomops.databinding.ItemDriverBinding
class DriverAdapter(
    private val drivers: List<Driver>,
    private val onCall: (Driver) -> Unit
) : RecyclerView.Adapter<DriverAdapter.VH>() {

    inner class VH(val binding: ItemDriverBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDriverBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val driver = drivers[position]
        with(holder.binding) {
            tvDriverLabel.text = if (drivers.size > 1) "DRIVER ${driver.sequence}" else "DRIVER"
            tvDriverName.text = driver.driverName.orEmpty()
            tvDriverMobile.text = driver.driverMobile.orEmpty()

            val hasMobile = !driver.driverMobile.isNullOrBlank()
            btnCall.visibility = if (hasMobile) View.VISIBLE else View.GONE
            btnCall.setOnClickListener { onCall(driver) }
        }
    }

    override fun getItemCount() = drivers.size
}