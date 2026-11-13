package com.example.atomops.models.localloadbox



// UI model for one row (immutable so DiffUtil works; use copy() to change)
data class LoadBoxItem(
    val boxCode: String,
    val docketNo: String,
    val scanned: Boolean = false,
    val scannedAt: String? = null,

    val isDamage: Boolean = false,
    val damageRemarks: String = "",
    val damageUrl: String = "",

    val isShort: Boolean = false,
    val shortRemarks: String = ""
)
