package com.example.atomops.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.atomops.database.daos.BarcodeDao
import com.example.atomops.database.daos.TripDetailsDao
import com.example.atomops.database.daos.UserDao
import com.example.atomops.database.tables.BarcodeTable
import com.example.atomops.database.tables.TripDetailsTable
import com.example.atomops.database.tables.User

@Database(entities = [User::class, TripDetailsTable::class, BarcodeTable::class], version = 1, exportSchema = true)
    abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tripDetailDao(): TripDetailsDao
    abstract fun getBarcodeDao(): BarcodeDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "Atom_Ops"
                ).build().also { INSTANCE = it }
            }
    }

    }
