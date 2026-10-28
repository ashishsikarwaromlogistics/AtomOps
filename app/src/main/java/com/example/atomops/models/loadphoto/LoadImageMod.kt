package com.example.atomops.models.loadphoto

data class LoadImageMod(
    val actor: String,
    val branchCode: String,
    val captureStage: String,
    val geoLat: Double,
    val geoLong: Double,
    val photoUrl: String
)