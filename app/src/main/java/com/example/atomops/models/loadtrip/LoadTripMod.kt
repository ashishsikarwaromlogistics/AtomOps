package com.example.atomops.models.loadtrip

data class LoadTripMod(
    val actor: String,
    val branch: String,
    val companyCode: Int,
    val items: List<Item>,
    val shortBoxes: List<ShortBoxe>
)