package com.example.atomops.models.bulkids

data class Item(
    val boxCode: String,
    val branchCode: String,
    val clientRef: String,
    val companyCode: String,
    val deviceId: String,
    val eventType: String,
    val expectedDocketNo: String,
    val locationCode: String,
    val scanStage: String,
    val scannedAt: String,
    val scannedBy: String
)