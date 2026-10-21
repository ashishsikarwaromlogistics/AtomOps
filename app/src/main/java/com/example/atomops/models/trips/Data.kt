package com.example.atomops.models.trips

data class Data(
    val items: List<Item>,
    val page: Int,
    val size: Int,
    val total: Int
)