package com.example.atomops.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LocalLoadbarcode")
data class LocalLoadbarcode(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "trip_id")
    val tripId: String? = null,

    @ColumnInfo(name = "barcodebox")
    val barcodebox: String? = null,

    @ColumnInfo(name = "docket")
    val docket: String? = null,

    @ColumnInfo(name = "scan_status")
    val scanStatus: String? = null,

    @ColumnInfo(name = "scan_at")
    val scanAt: String? = null,

    @ColumnInfo(name = "scan_id")
    val scanId: String? = null,

    @ColumnInfo(name = "event_id")
    val eventId: String? = null,


    @ColumnInfo(name = "isDamage")
    var isDamage: String="",

    @ColumnInfo(name = "damageURL")
    var damageURL: String="",

    @ColumnInfo(name = "shortremarks")
    var shortremarks: String="",

    @ColumnInfo(name = "damageremarks")
    var damageremarks: String=""


)