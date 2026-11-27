package com.example.atomops.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AcessBranches")
data class AcessBranches(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val branch: String)