package com.example.atomops.models.localtrips

data class Summary(
    val activeTrips: Int,
    val deliveredCnCount: Int,
    val pickedPrqCount: Int,
    val totalCnCount: Int,
    val totalPrqCount: Int
)