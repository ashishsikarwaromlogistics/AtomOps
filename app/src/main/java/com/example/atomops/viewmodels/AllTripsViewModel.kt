package com.example.atomops.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.atomops.data.DummyTripData
import com.example.atomops.models.Trip
import com.example.atomops.models.TripStatus

enum class TripTab(val label: String, val status: TripStatus?) {
    ALL("All Trips", null),
    LOADING("Loading", TripStatus.LOADING),
    UNLOADING("Unloading", TripStatus.UNLOADING),
    COMPLETED("Completed", TripStatus.COMPLETED)
}

class AllTripsViewModel : ViewModel() {

    /** Full, unfiltered dummy dataset. Swap for a repository call later. */
    private val allTrips: List<Trip> = DummyTripData.getTrips()

    var searchQuery by mutableStateOf("")
        private set

    var selectedTab by mutableStateOf(TripTab.ALL)
        private set

    val totalCount: Int get() = allTrips.size
    val loadingCount: Int get() = allTrips.count { it.status == TripStatus.LOADING }
    val unloadingCount: Int get() = allTrips.count { it.status == TripStatus.UNLOADING }
    val completedCount: Int get() = allTrips.count { it.status == TripStatus.COMPLETED }

    val filteredTrips: List<Trip>
        get() {
            val statusFiltered = selectedTab.status?.let { status ->
                allTrips.filter { it.status == status }
            } ?: allTrips

            return if (searchQuery.isBlank()) {
                statusFiltered
            } else {
                statusFiltered.filter { it.tripId.contains(searchQuery, ignoreCase = true) }
            }
        }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    fun onTabSelected(tab: TripTab) {
        selectedTab = tab
    }
}
