package com.example.atomops.models.document

data class Load(
    val boxCount: Int,
    val docketCount: Int,
    val manifestCount: Int,
    val totalWeightKg: Int,
    val utilizationPercent: Double
)