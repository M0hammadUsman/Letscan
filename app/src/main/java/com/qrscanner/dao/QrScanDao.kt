package com.qrscanner.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.qrscanner.entity.QrScan
import kotlinx.coroutines.flow.Flow

@Dao
interface QrScanDao {
    @Insert
    suspend fun insertScan(scan: QrScan)

    @Query("SELECT * FROM qr_scan ORDER BY scanDateTime DESC")
    fun getAllScans(): Flow<List<QrScan>>

    @Query("SELECT * FROM qr_scan WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): QrScan?

    @Delete
    suspend fun deleteScan(scan: QrScan)

    @Query("DELETE FROM qr_scan")
    suspend fun deleteAllScans()
}

