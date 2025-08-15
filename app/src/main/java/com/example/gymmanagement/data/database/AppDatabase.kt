package com.example.gymmanagement.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Member::class],
    version = 3,
    exportSchema = false // Set to false to avoid schema validation during development
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_management_database"
                )
                    .fallbackToDestructiveMigration() // This will prevent crashes on schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}