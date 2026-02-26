package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String,
    val logoUrl: String?,
    val phone: String?,
    val updatedAt: Long
)
