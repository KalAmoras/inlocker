package com.kalsys.inlocker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitor")
data class Monitor(
    @PrimaryKey val id: Int = 1,
    val shouldMonitor: Boolean
)
