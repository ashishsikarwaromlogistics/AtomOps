package com.example.atomops.models.findbox

data class Boxe(
    val boxCode: String,
    val currentBranch: String,
    val seq: Int,
    val status: String,
    val voided: Boolean
)