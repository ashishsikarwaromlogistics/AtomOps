package com.example.atomops.models.localtripunload

data class Delivery(
    val boxId: String? = null,
    val cnStatus: String? = null,
    val docketNo: String? = null,
    val exceptionId: Int? = null,          // null in the response
    val exceptionStatus: String? = null,   // null in the response
    val rewarehoused: Boolean? = null
)
