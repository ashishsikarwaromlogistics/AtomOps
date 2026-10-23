package com.example.atomops.models.dummy

enum class GateMode { GATE_IN, GATE_OUT }

data class GateTrip(
    val id: String,
    val createdAt: String,
    val status: TripStatus,
    val stops: List<String>,
    val serviceName: String,
    val manifestId: String,
    val driverName: String,
    val vehicleNo: String,
    val volWt: Int,
    val actWt: Int,
    val eta: String,
    val mode: GateMode,
    val isDone: Boolean          // false = Upcoming, true = Checked-In / Checked-Out
) {
    val routeText: String get() = stops.joinToString(" → ")
    val manifestLabel: String get() = "ID - $manifestId"
    val weightLabel: String get() = "${volWt}KG/${actWt}KG"
}