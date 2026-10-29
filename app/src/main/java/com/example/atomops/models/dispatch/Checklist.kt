package com.example.atomops.models.dispatch

data class Checklist(
    val digitalLockOk: Boolean,
    val gpsOk: Boolean,
    val lashingOk: Boolean,
    val tarpaulinOk: Boolean,
    val vehicleTypeOk: Boolean
)