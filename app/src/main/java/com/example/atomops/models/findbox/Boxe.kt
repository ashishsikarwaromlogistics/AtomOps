package com.example.atomops.models.findbox

data class Boxe(
    val boxCode: String,
    val currentBranch: String,
    val seq: Int,
    val status: String,
    val scanAction: String,
    val scanActionReason: String,
    val lastMovement : LastMovement,
    val voided: Boolean
)

data class LastMovement(
    val scanEventId:String,
    val eventType:String,
    val branchCode:String,
    val scannedAt:String,
)