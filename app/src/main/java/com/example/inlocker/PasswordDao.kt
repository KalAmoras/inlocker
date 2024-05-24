package com.example.inlocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PasswordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(passwordItem: PasswordItem): Long

    @Update
    suspend fun update(passwordItem: PasswordItem)

    @Query("SELECT * FROM password_items WHERE chosenApp = :chosenApp LIMIT 1")
    suspend fun getPasswordItem(chosenApp: String): PasswordItem?

    @Query("SELECT * FROM password_items")
    suspend fun getAllPasswords(): List<PasswordItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(passwordItem: PasswordItem)
}