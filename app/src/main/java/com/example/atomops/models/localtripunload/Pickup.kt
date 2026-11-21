package com.example.atomops.models.localtripunload

data class Pickup(
    val boxesOutstanding: Int? = null,
    val collectedBoxes: Int? = null,
    val dockets: List<Docket>? = null,
    val prqNo: String? = null,
    val prqStatus: String? = null,
    val prsNo: String? = null,
    val receivedBoxes: Int? = null,        // null in the response
    val scannedBoxes: Int? = null,
    val shortExceptionId: Int? = null,     // null in the response
    val shortExceptionStatus: String? = null
)
