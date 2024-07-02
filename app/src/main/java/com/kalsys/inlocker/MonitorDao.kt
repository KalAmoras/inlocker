package com.kalsys.inlocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MonitorDao {

    @Query("SELECT * FROM monitor WHERE id = 1")
    suspend fun getMonitor(): Monitor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitor(monitor: Monitor)

    @Update
    suspend fun updateMonitor(monitor: Monitor)
}