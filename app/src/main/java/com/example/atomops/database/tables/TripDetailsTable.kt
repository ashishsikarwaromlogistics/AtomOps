package com.example.atomops.database.tables


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "TripDetailsTable"
)
data class TripDetailsTable(
    // Manifest level
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val manifestNo: String,
    val legSequence: Int = 0,
    val sourceBranch: String? = null,
    val destinationBranch: String? = null,
    val cnCount: Int = 0,
    val boxCountTotal: Int = 0,
    val status: String? = null,
    val boxesScanned: Int = 0,
    val boxesUnloaded: Int = 0,
    @ColumnInfo(name = "trip_id")
    val tripId: String? = null,

    // Docket level
    val docketNo: String,
    val boxes: Int = 0,
    val weightKg: Double = 0.0,
    val serviceMode: String? = null,
    val agingHours: Int = 0,
    val documentStatus: String? = null,
    val missingMarkedAt: String? = null,
    val missingMarkedBy: String? = null,
    val fragile: Boolean = false,
    val weightCheckRequired: Boolean = false,
    val liquidOil: Boolean = false
)