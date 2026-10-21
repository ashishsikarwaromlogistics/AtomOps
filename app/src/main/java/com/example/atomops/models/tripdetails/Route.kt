package com.example.atomops.models.tripdetails

data class Route(
    val destinationBranch: String,
    val distanceKm: Int,
    val routeCode: String,
    val routeType: String,
    val sourceBranch: String,
    val touchPointCodes: List<String>
)