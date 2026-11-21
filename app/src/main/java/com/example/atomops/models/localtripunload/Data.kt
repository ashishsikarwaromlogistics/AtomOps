package com.example.atomops.models.localtripunload

data class Data(
    val boxesNotBack: Int? = null,
    val boxesOnRecord: Int? = null,
    val delivery: List<Delivery>? = null,
    val pending: List<Pending>? = null,
    val pickupBoxesShort: Int? = null,
    val pickups: List<Pickup>? = null,
    val tripNo: String? = null,
    val tripStatus: String? = null
)
