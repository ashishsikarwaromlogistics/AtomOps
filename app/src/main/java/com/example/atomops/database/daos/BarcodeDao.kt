package com.example.atomops.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.atomops.database.tables.BarcodeTable
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BarcodeTable): Long


    @Query("select count(*) from BarcodeTable where box=:boxno")
    suspend fun checkexist(boxno:String): Long

    @Query("select isScan from BarcodeTable where box=:boxno")
    suspend fun checkScan(boxno:String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BarcodeTable>)

    // ---------- READ ----------
    @Query("SELECT * FROM BarcodeTable")
    suspend fun getAll(): List<BarcodeTable>


    @Query("SELECT * FROM BarcodeTable WHERE docketno = :docket ")
    suspend fun getAllBox(docket: String): List<BarcodeTable>

    @Query("SELECT * FROM BarcodeTable ORDER BY id DESC")
    fun observeAll(): Flow<List<BarcodeTable>>

    @Query("SELECT * FROM BarcodeTable WHERE id = :id")
    suspend fun getById(id: Int): BarcodeTable?

    @Query("SELECT * FROM BarcodeTable WHERE trip_id = :tripId ORDER BY id DESC")
    suspend fun getByTrip(tripId: String): List<BarcodeTable>

    @Query("SELECT * FROM BarcodeTable WHERE trip_id = :tripId ORDER BY id DESC")
    fun observeByTrip(tripId: String): Flow<List<BarcodeTable>>

    @Query("SELECT * FROM BarcodeTable WHERE manifest = :manifest")
    suspend fun getByManifest(manifest: String): List<BarcodeTable>

    @Query("SELECT * FROM BarcodeTable WHERE manifest = :manifest and isScan=0 and isshort!='' ")
    suspend fun getByManifestshort(manifest: String): List<BarcodeTable>

    @Query("SELECT * FROM BarcodeTable WHERE manifest = :manifest and isScan=1 and isshort='' ")
    suspend fun getByManifestwithoutshort(manifest: String): List<BarcodeTable>

    @Query("SELECT * FROM BarcodeTable WHERE manifest = :manifest")
    fun observeByManifest(manifest: String): Flow<List<BarcodeTable>>

    @Query("SELECT * FROM BarcodeTable WHERE box = :box AND trip_id = :tripId LIMIT 1")
    suspend fun getByBox(box: String, tripId: String): BarcodeTable?

    @Query("SELECT * FROM BarcodeTable WHERE trip_id = :tripId AND status = :status")
    suspend fun getByStatus(tripId: String, status: Boolean): List<BarcodeTable>

    // ---------- COUNT / CHECK ----------
    @Query("SELECT COUNT(*) FROM BarcodeTable WHERE trip_id = :tripId")
    suspend fun countByTrip(tripId: String): Int

    @Query("SELECT COUNT(*) FROM BarcodeTable WHERE trip_id = :tripId")
    fun observeCountByTrip(tripId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM BarcodeTable WHERE manifest = :manifest")
    suspend fun countByManifest(manifest: String): Int

    @Query("SELECT COUNT(*) FROM BarcodeTable WHERE manifest = :manifest AND status = 1")
    suspend fun countScanned(manifest: String): Int

    @Query("SELECT COUNT(*) FROM BarcodeTable WHERE manifest = :manifest AND status = 1")
    fun observeCountScanned(manifest: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM BarcodeTable WHERE box = :box AND trip_id = :tripId)")
    suspend fun isScanned(box: String, tripId: String): Boolean

    // ---------- UPDATE ----------
    @Update
    suspend fun update(item: BarcodeTable): Int

    @Update
    suspend fun updateAll(items: List<BarcodeTable>): Int

    @Query("UPDATE BarcodeTable SET isScan = :isScan WHERE box = :box AND manifest = :manfiest")
    suspend fun updateStatus(box: String, manfiest: String, isScan: Boolean): Int


    @Query("UPDATE BarcodeTable SET boxloaded = 1 WHERE  manifest = :manfiest")
    suspend fun updateLoadstatus(manfiest: String): Int

    @Query("""
        SELECT NOT EXISTS (
            SELECT 1 FROM BarcodeTable
            WHERE trip_id = :tripId AND boxloaded = 0
        )
    """)
    suspend fun areAllBoxesLoaded(tripId: String): Boolean

    @Query("""
    SELECT box FROM BarcodeTable 
    WHERE manifest = :manifestNo 
    AND isScan = 0 
    AND isshort = ''
""")
    suspend fun getPendingBoxes(manifestNo: String): List<String>

    @Query("UPDATE BarcodeTable SET eventid = :eventid WHERE box = :box")
    suspend fun updateeventid(box: String, eventid: String): Int

    @Query("UPDATE BarcodeTable SET isshort = :isshort WHERE box = :box ")
    suspend fun updateShortStatus(box: String, isshort: String): Int

    @Query("UPDATE BarcodeTable SET damage = :damage WHERE box = :box ")
    suspend fun updateDamageStatus(box: String, damage: String): Int

    @Query("UPDATE BarcodeTable SET isshort = '' WHERE box = :box ")
    suspend fun removeshortShortStatus(box: String): Int

    @Query("UPDATE BarcodeTable SET status = :status WHERE manifest = :manifest")
    suspend fun updateStatusByManifest(manifest: String, status: Boolean)

    @Query("UPDATE BarcodeTable SET timeScan = :timeScan WHERE id = :id")
    suspend fun updateTimeScan(id: Int, timeScan: String)

    // ---------- DELETE ----------
    @Delete
    suspend fun delete(item: BarcodeTable): Int

    @Query("DELETE FROM BarcodeTable WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM BarcodeTable WHERE box = :box AND trip_id = :tripId")
    suspend fun deleteByBox(box: String, tripId: String)

    @Query("UPDATE BarcodeTable SET status = :status WHERE box = :box AND trip_id = :tripId")
    suspend fun UpdateByBox(box: String, tripId: String, status: Boolean)

    @Query("UPDATE BarcodeTable SET status = :status, timeScan=:timeScan  WHERE box = :box ")
    suspend fun UpdateByBoxwithtime(box: String,timeScan:String, status: Boolean): Int


    @Query("DELETE FROM BarcodeTable WHERE trip_id = :tripId")
    suspend fun deleteByTrip(tripId: String)

    @Query("DELETE FROM BarcodeTable WHERE manifest = :manifest")
    suspend fun deleteByManifest(manifest: String)

    @Query("DELETE FROM BarcodeTable")
    suspend fun clear()

    // ---------- COMBINED ----------
    /**
     * Inserts a scan only if this box was not already scanned for the trip.
     * Returns the new row id, or -1 if it was a duplicate.
     */
    @Transaction
    suspend fun insertIfNew(item: BarcodeTable): Long {
        return if (isScanned(item.box, item.tripId)) -1L else insert(item)
    }
}