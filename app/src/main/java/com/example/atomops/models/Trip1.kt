package com.example.atomops.models

data class Trip1(
    val tripId: String,
    val dateTime: String,
    val status: String,      // Created, Loading, Unloading, Completed
    val route: String,
    val service: String,
    val dock: String,
    val vehicleNo: String
)