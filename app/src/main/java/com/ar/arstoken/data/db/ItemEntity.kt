package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cloudId: String = "",
    val name: String,
    val price: Int,
    val category: String? = null,
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)
