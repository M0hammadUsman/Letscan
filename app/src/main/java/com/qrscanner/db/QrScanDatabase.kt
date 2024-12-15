package com.qrscanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.qrscanner.dao.QrScanDao
import com.qrscanner.entity.QrScan

@Database(entities = [QrScan::class], version = 2, exportSchema = false)
abstract class QrScanDatabase : RoomDatabase() {
    abstract fun qrScanDao(): QrScanDao

    companion object {
        private var instance: QrScanDatabase? = null

        fun getDatabase(context: Context): QrScanDatabase {
            return instance ?: synchronized(this) {
                var instance = Room.databaseBuilder(
                    context.applicationContext,
                    QrScanDatabase::class.java,
                    "qr_scan_database"
                ).build()
                instance.also { db -> instance = db }
            }
        }
    }
}


