package com.kalsys.inlocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocation(): LocationEntity?
}
