package com.example.atomops.models.tripdetails

data class Vehicle(
    val capacityKg: Int,
    val digitalLock: Boolean,
    val gpsStatus: String,
    val ownership: String,
    val vehicleNo: String,
    val vehicleType: String,
    val vendorCode: String
)