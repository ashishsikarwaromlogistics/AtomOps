package com.example.atomops.models.tripdetails

data class Trip(
    val boxCount: Int,
    val createdAt: String,
    val createdBy: String,
    val currentDockNo: String,
    val destinationBranch: String,
    val digitalLock: Boolean,
    val docketCount: Int,
    val gpsStatus: String,
    val manifestCount: Int,
    val priority: String,
    val routeCode: String,
    val routeDistanceKm: Int,
    val routeType: String,
    val sourceBranch: String,
    val status: String,
    val statusChangedAt: String,
    val totalWeightKg: Int,
    val tripNo: String,
    val utilizationPercent: Double,
    val vehicleCapacityKg: Int,
    val vehicleNo: String,
    val vehicleOwnership: String,
    val vehicleType: String,
    val vendorCode: String
)