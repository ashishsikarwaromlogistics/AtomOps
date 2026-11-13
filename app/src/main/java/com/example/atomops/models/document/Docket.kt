package com.example.atomops.models.document

data class Docket(
    val agingHours: Int,
    val boxes: Int,
    val docketNo: String,
    val documentStatus: String,
    val fragile: Boolean,
    val liquidOil: Boolean,
    val missingMarkedAt: Any,
    val missingMarkedBy: Any,
    val serviceMode: String,
    val weightCheckRequired: Boolean,
    val weightKg: Int
)