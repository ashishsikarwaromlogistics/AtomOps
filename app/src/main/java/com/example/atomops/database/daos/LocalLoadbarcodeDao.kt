package com.example.atomops.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.atomops.database.tables.LocalLoadbarcode
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalLoadbarcodeDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LocalLoadbarcode): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocalLoadbarcode>): List<Long>

    // ---------- UPDATE ----------
    @Update
    suspend fun update(item: LocalLoadbarcode)



    // ---------- DELETE ----------
    @Delete
    suspend fun delete(item: LocalLoadbarcode)

    @Query("DELETE FROM LocalLoadbarcode")
    suspend fun deleteAll()

    @Query("DELETE FROM LocalLoadbarcode WHERE trip_id = :tripId")
    suspend fun deleteByTripId(tripId: String)

    // ---------- QUERY ----------
    @Query("SELECT * FROM LocalLoadbarcode ORDER BY id DESC")
    fun getAll(): Flow<List<LocalLoadbarcode>>

    @Query("SELECT * FROM LocalLoadbarcode ORDER BY id DESC")
    suspend fun getAllOnce(): List<LocalLoadbarcode>

    @Query("SELECT * FROM LocalLoadbarcode WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LocalLoadbarcode?

    @Query("SELECT * FROM LocalLoadbarcode WHERE trip_id = :tripId")
    fun getByTripId(tripId: String): Flow<List<LocalLoadbarcode>>

    @Query("SELECT * FROM LocalLoadbarcode WHERE barcodebox = :barcodebox LIMIT 1")
    suspend fun getByBarcode(barcodebox: String): LocalLoadbarcode?

    @Query("SELECT * FROM LocalLoadbarcode WHERE docket = :docket")
    suspend fun getByDocket(docket: String): List<LocalLoadbarcode>

    @Query("SELECT * FROM LocalLoadbarcode WHERE scan_status = :status")
    fun getByScanStatus(status: String): Flow<List<LocalLoadbarcode>>

    @Query("SELECT * FROM LocalLoadbarcode WHERE scan_id = :scanId LIMIT 1")
    suspend fun getByScanId(scanId: String): LocalLoadbarcode?

    @Query("SELECT * FROM LocalLoadbarcode WHERE event_id = :eventId LIMIT 1")
    suspend fun getByEventId(eventId: String): LocalLoadbarcode?

    @Query("SELECT COUNT(*) FROM LocalLoadbarcode")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM LocalLoadbarcode WHERE trip_id = :tripId")
    suspend fun getCountByTripId(tripId: String): Int

    // Check if a barcode already exists for a trip (avoid duplicates)
    @Query("SELECT EXISTS(SELECT 1 FROM LocalLoadbarcode WHERE trip_id = :tripId AND barcodebox = :barcodebox)")
    suspend fun exists(tripId: String, barcodebox: String): Boolean

    @Query("SELECT barcodebox FROM LocalLoadbarcode WHERE trip_id = :tripId")
    suspend fun getExistingBoxCodes(tripId: String): List<String>







    // Single box lookup (for markScanned)
    @Query("SELECT * FROM LocalLoadbarcode WHERE barcodebox = :boxCode AND trip_id = :tripId LIMIT 1")
    suspend fun getBox(boxCode: String, tripId: String): LocalLoadbarcode?

    // Update scan status on a box
    @Query("""
    UPDATE LocalLoadbarcode
    SET scan_status = :status, scan_at = :scanAt, scan_id = :scanId, event_id = :eventId
    WHERE barcodebox = :boxCode AND trip_id = :tripId
""")
    suspend fun updateScan(
        boxCode: String,
        tripId: String,
        status: String,
        scanAt: String,
        scanId: String,
        eventId: String
    ): Int

    @Query("""
    SELECT barcodebox FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND LOWER(scan_status) = 'scanned'
""")
    suspend fun getScannedBoxCodes(tripId: String): List<String>


    // ---- Scan ----
    @Query("""
    UPDATE LocalLoadbarcode
    SET scan_status = :scanStatus, scan_at = :scanAt, scan_id = :scanId
    WHERE barcodebox = :barcodebox AND trip_id = :tripId
""")
    suspend fun updateScanFieldsByBarcode(
        barcodebox: String, tripId: String,
        scanStatus: String?, scanAt: String?, scanId: String?
    ): Int





    // ---- Reads for detail screen ----
    @Query("SELECT * FROM LocalLoadbarcode WHERE docket = :docket AND trip_id = :tripId ORDER BY id ASC")
    suspend fun getByDocketAndTrip(docket: String, tripId: String): List<LocalLoadbarcode>




    // All boxes for the trip
    @Query("SELECT * FROM LocalLoadbarcode WHERE trip_id = :tripId")
    suspend fun getByTrip(tripId: String): List<LocalLoadbarcode>

    // Only invalid boxes: not scanned AND not short
    @Query("""
    SELECT * FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND LOWER(IFNULL(scan_status, '')) != 'scanned'
      AND IFNULL(shortremarks, '') = ''
""")
    suspend fun getPendingBoxes(tripId: String): List<LocalLoadbarcode>

    // Pending boxes grouped by docket (optional convenience)
    @Query("""
    SELECT docket, COUNT(*) AS count FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND LOWER(IFNULL(scan_status, '')) != 'scanned'
      AND IFNULL(shortremarks, '') = ''
    GROUP BY docket
""")
    suspend fun getPendingCountByDocket(tripId: String): List<DocketPendingCount>

    data class DocketPendingCount(
        val docket: String?,
        val count: Int
    )


    @Query("SELECT COUNT(*) FROM LocalLoadbarcode WHERE trip_id = :tripId")
    suspend fun getTotalCount(tripId: String): Int

    @Query("""
    SELECT COUNT(*) FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND LOWER(IFNULL(scan_status, '')) = 'scanned'
""")
    suspend fun getScannedCount(tripId: String): Int

    @Query("""
    SELECT COUNT(*) FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND IFNULL(shortremarks, '') != ''
""")
    suspend fun getShortCount(tripId: String): Int

    @Query("""
    SELECT COUNT(*) FROM LocalLoadbarcode
    WHERE trip_id = :tripId
      AND LOWER(IFNULL(isDamage, '')) = 'damage'
""")
    suspend fun getDamageCount(tripId: String): Int



    // ---- Damage ----
    @Query("""
    UPDATE LocalLoadbarcode
    SET isDamage      = 'Damage',
        damageremarks = :remarks,
        damageURL     = :imagePath
    WHERE barcodebox = :barcodebox AND trip_id = :tripId
""")
    suspend fun markDamage(
        barcodebox: String,
        tripId: String,
        remarks: String,
        imagePath: String
    ): Int

    @Query("""
    UPDATE LocalLoadbarcode
    SET isDamage = '', damageremarks = '', damageURL = ''
    WHERE barcodebox = :barcodebox AND trip_id = :tripId
""")
    suspend fun clearDamage(barcodebox: String, tripId: String): Int

    // ---- Short ----
    @Query("""
    UPDATE LocalLoadbarcode
    SET shortremarks = :remarks
    WHERE barcodebox = :barcodebox AND trip_id = :tripId
""")
    suspend fun markShort(
        barcodebox: String,
        tripId: String,
        remarks: String
    ): Int

    @Query("""
    UPDATE LocalLoadbarcode
    SET shortremarks = ''
    WHERE barcodebox = :barcodebox AND trip_id = :tripId
""")
    suspend fun clearShort(barcodebox: String, tripId: String): Int




}