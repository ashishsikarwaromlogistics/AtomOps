package com.example.atomops.models.dummy

import com.example.atomops.R

enum class TripStatus(
    val label: String,
    val bg: Int,
    val fg: Int,
    val dot: Int
) {
    CREATED("Created", R.color.created_bg, R.color.created_text, R.color.dot_created),
    DISPATCH("Dispatch", R.color.dispatch_bg, R.color.dispatch_text, R.color.dot_dispatch),
    GATE_OUT("Gate Out", R.color.pickup_bg, R.color.pickup_text, R.color.dot_gate_out),
    GATE_IN("Gate In", R.color.gate_in_bg, R.color.gate_in_text, R.color.dot_gate_in),
    COMPLETED("Completed", R.color.completed_bg, R.color.completed_text, R.color.dot_completed),
    CANCELLED("Cancelled", R.color.cancelled_bg, R.color.cancelled_text, R.color.dot_cancelled)
}

data class Trip(
    val id: String,
    val createdAt: String,
    val status: TripStatus,
    val stops: List<String>,
    val serviceTime: String,
    val dock: String,
    val manifestCount: Int,
    val vehicleNo: String,
    val ownership: String,
    val remainingKg: Int
) {
    val routeText: String get() = stops.joinToString(" → ")
    val serviceLabel: String get() = "Service ($serviceTime)"
    val dockLabel: String get() = "Dock - $dock"
    val manifestText: String get() = "%02d".format(manifestCount)
    val vehicleLabel: String get() = "$vehicleNo ($ownership)"
    val utilizationLabel: String get() = "${remainingKg}KG Remaining"
}

data class TripStat(
    val label: String,
    val count: Int,
    val status: TripStatus?      // null = "Total Trips"
) {
    val countText: String get() = "%02d".format(count)
}