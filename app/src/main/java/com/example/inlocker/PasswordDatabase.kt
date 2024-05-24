package com.example.inlocker

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database


@Database(entities = [PasswordItem::class], version = 1)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: PasswordDatabase? = null

        fun getInstance(context: Context): PasswordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
