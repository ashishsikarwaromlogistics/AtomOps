package com.example.atomops.models.localtrips

data class Data(
    val items: List<Item>,
    val page: Int,
    val size: Int,
    val summary: Summary,
    val total: Int
)