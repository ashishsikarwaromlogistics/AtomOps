package com.example.atomops.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.atomops.database.tables.TripDetailsTable
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDetailsDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TripDetailsTable): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TripDetailsTable>)

    // ---------- READ ----------
    @Query("SELECT * FROM TripDetailsTable")
    suspend fun getAll(): List<TripDetailsTable>

    @Query("SELECT EXISTS (SELECT 1 FROM TripDetailsTable WHERE docketNo = :docketNo)")
    suspend fun existsDocket(docketNo: String): Boolean

    @Query("SELECT * FROM TripDetailsTable")
    fun observeAll(): Flow<List<TripDetailsTable>>

    @Query("SELECT * FROM TripDetailsTable WHERE id = :id")
    suspend fun getById(id: Int): TripDetailsTable?

    @Query("SELECT * FROM TripDetailsTable WHERE trip_id = :tripId")
    suspend fun getByTrip(tripId: String): List<TripDetailsTable>

    @Query("""
    SELECT manifestNo
    FROM TripDetailsTable
    WHERE trip_id = :tripId
    GROUP BY manifestNo
""")
    suspend fun getByTripandmanifiest(
        tripId: String
    ): List<String>

    @Query("SELECT * FROM TripDetailsTable WHERE trip_id = :tripId")
    fun observeByTrip(tripId: String): Flow<List<TripDetailsTable>>

    @Query("SELECT * FROM TripDetailsTable WHERE manifestNo = :manifestNo")
    suspend fun getByManifest(manifestNo: String): List<TripDetailsTable>

    @Query("SELECT * FROM TripDetailsTable WHERE docketNo = :docketNo LIMIT 1")
    suspend fun getByDocket(docketNo: String): TripDetailsTable?

    @Query("SELECT DISTINCT manifestNo FROM TripDetailsTable WHERE trip_id = :tripId")
    suspend fun getManifestNos(tripId: String): List<String>

    @Query("SELECT COUNT(*) FROM TripDetailsTable WHERE trip_id = :tripId")
    suspend fun countByTrip(tripId: String): Int

    // ---------- UPDATE ----------
    @Update
    suspend fun update(item: TripDetailsTable): Int

    @Update
    suspend fun updateAll(items: List<TripDetailsTable>): Int

    @Query("UPDATE TripDetailsTable SET boxesScanned = :scanned WHERE manifestNo = :manifestNo")
    suspend fun updateBoxesScanned(manifestNo: String, scanned: Int)

    @Query("UPDATE TripDetailsTable SET boxesUnloaded = :unloaded WHERE manifestNo = :manifestNo")
    suspend fun updateBoxesUnloaded(manifestNo: String, unloaded: Int)

    @Query("UPDATE TripDetailsTable SET status = :status WHERE manifestNo = :manifestNo")
    suspend fun updateManifestStatus(manifestNo: String, status: String)

    @Query(
        "UPDATE TripDetailsTable SET missingMarkedAt = :markedAt, missingMarkedBy = :markedBy " +
                "WHERE manifestNo = :manifestNo AND docketNo = :docketNo"
    )
    suspend fun markDocketMissing(
        manifestNo: String,
        docketNo: String,
        markedAt: String,
        markedBy: String
    )

    // ---------- DELETE ----------
    @Delete
    suspend fun delete(item: TripDetailsTable): Int

    @Query("DELETE FROM TripDetailsTable WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM TripDetailsTable WHERE trip_id = :tripId")
    suspend fun deleteByTrip(tripId: String)

    @Query("DELETE FROM TripDetailsTable WHERE manifestNo = :manifestNo")
    suspend fun deleteByManifest(manifestNo: String)

    @Query("DELETE FROM TripDetailsTable")
    suspend fun clear()

    // ---------- COMBINED (avoids duplicates) ----------
    @Transaction
    suspend fun replaceForTrip(tripId: String, items: List<TripDetailsTable>) {
        deleteByTrip(tripId)
        insertAll(items)
    }
}