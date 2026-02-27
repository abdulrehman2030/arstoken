package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cloudId: String = "",
    val name: String,
    val phone: String,
    val creditBalance: Int = 0,
    val updatedAt: Long = 0
)
