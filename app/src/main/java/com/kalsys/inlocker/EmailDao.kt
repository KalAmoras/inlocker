package com.kalsys.inlocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emailEntity: EmailEntity)

    @Query("SELECT * FROM email_table LIMIT 1")
    suspend fun getEmail(): EmailEntity?
}
