package com.example.atomops.models

data class DeviceSession(
    val id: String,
    val deviceName: String,     // "Chrome on Windows"
    val version: String,        // "v124.0.0.0"
    val location: String,       // "Bangalore, India"
    val isActiveNow: Boolean,
    val lastActive: String? = null // "2 days ago" (used when isActiveNow == false)
)
