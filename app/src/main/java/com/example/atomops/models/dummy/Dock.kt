package com.example.atomops.models.dummy

import com.example.atomops.R

enum class DockDirection(val label: String) {
    OUTBOUND("Outbound"),
    INBOUND("Inbound")
}

enum class SlotStatus(val label: String, val dot: Int) {
    AVAILABLE("Available", R.color.dot_completed),
    LOADING("Loading", R.color.dot_gate_in),
    UNLOADING("Unloading", R.color.dot_cancelled),
    INACTIVE("Inactive", R.color.dot_idle)
}

data class DockSlot(
    val name: String,             // "A. Dock Slot"
    val status: SlotStatus,
    val vehicleNo: String?,       // null = no vehicle docked
    val progress: Int             // 0..100
) {
    val vehicleText: String get() = vehicleNo ?: "- - - -"
    val progressText: String get() = "$progress%"
}

data class Dock(
    val id: String,
    val name: String,             // "Dock 02"
    val direction: DockDirection,
    val slots: List<DockSlot>
) {
    val availableCount: Int get() = slots.count { it.status == SlotStatus.AVAILABLE }
    val slotText: String get() = "$availableCount/${slots.size} SLOT AVAILABLE"
    val hasVehicle: Boolean get() = slots.any { it.vehicleNo != null }
}

data class DockStat(
    val label: String,
    val count: Int,
    val dotColor: Int
) {
    val countText: String get() = "%02d".format(count)
}