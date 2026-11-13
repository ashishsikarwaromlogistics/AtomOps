package com.example.atomops.localclasses

data class SaveValidationResult(
    val canSave: Boolean,
    val totalBoxes: Int,
    val scannedBoxes: Int,
    val shortBoxes: Int,
    val pendingBoxes: Int,
    val pendingByDocket: Map<String, List<String>>  // docketNo -> [boxCodes]
)