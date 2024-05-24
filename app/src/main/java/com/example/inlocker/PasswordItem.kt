package com.example.inlocker
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_items")
data class PasswordItem(
    @PrimaryKey val chosenApp: String,
    val password: String
)
