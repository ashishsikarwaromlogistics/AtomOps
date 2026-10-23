package com.example.atomops.models.bulkids

data class Result(
    val boxCode: String,
    val boxStatus: Any,
    val clientRef: String,
    val errorCode: String,
    val outcome: String,
    val publicEventId: Any
)