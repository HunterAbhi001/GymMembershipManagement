package com.example.gymmanagement.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(entities = [Member::class, CheckIn::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun checkInDao(): CheckInDao

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
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Add some dummy data for demonstration
                            CoroutineScope(Dispatchers.IO).launch {
                                val calendar = Calendar.getInstance()
                                val startDate = calendar.timeInMillis

                                calendar.add(Calendar.MONTH, 6)
                                val expiryDate1 = calendar.timeInMillis

                                calendar.timeInMillis = startDate
                                calendar.add(Calendar.DAY_OF_YEAR, 5)
                                val expiryDate2 = calendar.timeInMillis

                                calendar.timeInMillis = startDate
                                calendar.add(Calendar.YEAR, -1)
                                val expiryDate3 = calendar.timeInMillis

                                getDatabase(context).memberDao().upsertMember(Member(name="John Wick", contact="+911112223333", plan="6 Months", startDate=startDate, expiryDate=expiryDate1))
                                getDatabase(context).memberDao().upsertMember(Member(name="Jane Smith", contact="+914445556666", plan="1 Month", startDate=startDate, expiryDate=expiryDate2))
                                getDatabase(context).memberDao().upsertMember(Member(name="Peter Parker", contact="7778889999", plan="1 Year", startDate=startDate, expiryDate=expiryDate3))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}