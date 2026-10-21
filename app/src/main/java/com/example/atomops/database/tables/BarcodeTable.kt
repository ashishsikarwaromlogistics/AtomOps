package com.example.atomops.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "BarcodeTable")
data class BarcodeTable(
    @PrimaryKey(autoGenerate = true)
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
    @ColumnInfo(name = "trip_id")
    val tripId: String,

    var isScan: Boolean = false
)

