package com.example.atomops.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.atomops.database.tables.AcessBranches

@Dao
interface AcessBranchesDao {

    // Insert one row at a time
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(branch: AcessBranches): Long

    // Delete all rows
    @Query("DELETE FROM AcessBranches")
    suspend fun deleteAll()

    // Select all rows (one-time read)
    @Query("SELECT * FROM AcessBranches")
    suspend fun getAll(): List<AcessBranches>


}