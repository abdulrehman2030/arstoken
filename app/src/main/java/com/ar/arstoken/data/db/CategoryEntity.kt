package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cloudId: String = "",
    val name: String,
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)
