package com.example.atomops.activities

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey


object BoxStatus {
    const val NONE = ""
    const val SHORT = "SHORT"
    const val DAMAGE = "DAMAGE"
    const val EXCESS = "EXCESS"
}
data class BoxItem(

    val id: Int = 0,
    val docketno: String,
    val box: String,
    var timeScan: String,
    var status: String,
    var voided: Boolean,
    val manifest: String,
    val isshort: String,
    val damage: String,
    val damageurl: String,
    val tripId: String,
    var isScan: Boolean = false
)
