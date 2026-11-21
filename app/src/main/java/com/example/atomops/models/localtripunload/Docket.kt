package com.example.atomops.models.localtripunload

data class Docket(
    val boxes: List<Boxe>? = null,         // null in the response (e.g. B2600000042)
    val boxesToScan: Int? = null,          // null in the response
    val docketNo: String? = null,
    val scannedBoxes: Int? = null
)
