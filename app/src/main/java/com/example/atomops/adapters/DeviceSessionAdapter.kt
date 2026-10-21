package com.example.atomops.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atomops.R
import com.example.atomops.databinding.ItemDeviceSessionBinding
import com.example.atomops.models.DeviceSession

class DeviceSessionAdapter(
    private val onLogout: (DeviceSession) -> Unit
) : ListAdapter<DeviceSession, DeviceSessionAdapter.VH>(Diff) {

    inner class VH(private val b: ItemDeviceSessionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DeviceSession) {
            val ctx = b.root.context
            b.tvDevice.text = "${item.deviceName} (${item.version})"
            b.tvLocation.text = item.location

            if (item.isActiveNow) {
                b.tvStatus.text = "● Active Now"
                b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_green))
            } else {
                b.tvStatus.text = "● Last Active ${item.lastActive.orEmpty()}"
                b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            }
            b.btnLogout.setOnClickListener { onLogout(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDeviceSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    private object Diff : DiffUtil.ItemCallback<DeviceSession>() {
        override fun areItemsTheSame(a: DeviceSession, b: DeviceSession) = a.id == b.id
        override fun areContentsTheSame(a: DeviceSession, b: DeviceSession) = a == b
    }
}
