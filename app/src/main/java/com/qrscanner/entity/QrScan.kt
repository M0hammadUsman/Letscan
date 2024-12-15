package com.qrscanner.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_scan")
data class QrScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val isUrl: Boolean,
    val scanDateTime: Long = System.currentTimeMillis()
)

