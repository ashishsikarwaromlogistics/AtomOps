package com.example.atomops.models.tripdetails

data class Docket(
    val agingHours: Int,
    val boxes: Int,
    val docketNo: String,
    val documentStatus: String,
    val fragile: Boolean,
    val liquidOil: Boolean,
    val missingMarkedAt: String,
    val missingMarkedBy: String,
    val serviceMode: String,
    val weightCheckRequired: Boolean,
    val weightKg: Int
)