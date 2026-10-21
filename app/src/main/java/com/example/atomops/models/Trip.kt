package com.example.atomops.models

/**
 * Status of a trip, mirrors the tabs on the All Trips screen.
 */
enum class TripStatus(val label: String) {
    LOADING("Loading"),
    UNLOADING("Unloading"),
    COMPLETED("Completed"),
    CREATED("Created")
}

/**
 * Simple data model for a single trip card.
 */
data class Trip(
    val tripId: String,
    val dateTime: String,
    val status: TripStatus,
    val origin: String,
    val via: String,
    val destination: String,
    val dock: String,
    val vehicleNumber: String,
    val serviceLabel: String = "Service"
) {
    /** Formatted "MUM-MHBI-006 → PUN-MHBI-007 → DEL-MHBI-008" style route string. */
    val routeText: String
        get() = "$origin → $via → $destination"
}
