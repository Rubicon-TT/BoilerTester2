package com.example.boilertester

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BoilerSetup::class], version = 1, exportSchema = false)
abstract class BoilerDatabase : RoomDatabase() {
    abstract fun boilerSetupDao(): BoilerSetupDao

    companion object {
        @Volatile
        private var INSTANCE: BoilerDatabase? = null

        fun getDatabase(context: Context): BoilerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BoilerDatabase::class.java,
                    "boiler_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}