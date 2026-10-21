package com.example.atomops.models.tripdetails

data class Manifest(
    val boxCountTotal: Int,
    val boxesScanned: Int,
    val boxesUnloaded: Int,
    val cnCount: Int,
    val destinationBranch: String,
    val dockets: List<Docket>,
    val legSequence: Int,
    val manifestNo: String,
    val sourceBranch: String,
    val status: String
)