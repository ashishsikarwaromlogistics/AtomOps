package com.example.atomops.models.savedata

data class SaveDataMod(
    val actor: String,
    val branch: String,
    val companyCode: Int,
    val items: List<Item>,
    val shortBoxes: List<ShortBoxe>
)