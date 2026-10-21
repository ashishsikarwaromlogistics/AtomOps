package com.example.atomops.models.tripdetails

data class Driver(
    val driverCode: String,
    val driverMobile: String,
    val driverName: String,
    val gpsConsentState: String,
    val mobileVerified: Boolean,
    val sequence: Int
)