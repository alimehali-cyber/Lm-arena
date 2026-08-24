package com.alijafari.red.astronomy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_occasions")
data class UserOccasionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val timestampMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
