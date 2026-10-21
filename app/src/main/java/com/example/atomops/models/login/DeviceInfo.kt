package com.example.atomops.models.login

data class DeviceInfo(
    val deviceId: String,
    val deviceType: String,
    val ipAddress: String,
    val userAgent: String
)