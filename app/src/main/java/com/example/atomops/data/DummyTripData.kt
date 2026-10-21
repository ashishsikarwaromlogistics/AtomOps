package com.example.atomops.data

import com.example.atomops.models.Trip
import com.example.atomops.models.TripStatus

/**
 * Temporary in-memory dummy data for the All Trips screen.
 * Replace with a real repository / API call when the backend is ready.
 */
object DummyTripData {

    fun getTrips(): List<Trip> = listOf(
        Trip(
            tripId = "TRIP123456725",
            dateTime = "14-01-2025, 14:30",
            status = TripStatus.CREATED,
            origin = "MUM-MHBI-006",
            via = "PUN-MHBI-007",
            destination = "DEL-MHBI-008",
            dock = "Dock - 2A",
            vehicleNumber = "DL14CC 1586"
        ),
        Trip(
            tripId = "TRIP123456726",
            dateTime = "14-01-2025, 15:05",
            status = TripStatus.LOADING,
            origin = "MUM-MHBI-006",
            via = "PUN-MHBI-007",
            destination = "DEL-MHBI-008",
            dock = "Dock - 2A",
            vehicleNumber = "DL14CC 1586"
        ),
        Trip(
            tripId = "TRIP123456727",
            dateTime = "14-01-2025, 16:10",
            status = TripStatus.UNLOADING,
            origin = "MUM-MHBI-006",
            via = "PUN-MHBI-007",
            destination = "DEL-MHBI-008",
            dock = "Dock - 2A",
            vehicleNumber = "DL14CC 1586"
        ),
        Trip(
            tripId = "TRIP123456728",
            dateTime = "14-01-2025, 17:45",
            status = TripStatus.COMPLETED,
            origin = "MUM-MHBI-006",
            via = "PUN-MHBI-007",
            destination = "DEL-MHBI-008",
            dock = "Dock - 2A",
            vehicleNumber = "DL14CC 1586"
        )
    )
}