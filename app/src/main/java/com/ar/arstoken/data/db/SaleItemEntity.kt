package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cloudId: String = "",
    val saleId: Int,
    val saleCloudId: String = "",
    val itemId: Int,
    val itemCloudId: String = "",
    val itemName: String,
    val quantity: Double,
    val unitPrice: Int,
    val totalPrice: Double,
    val timestamp: Long,
    val updatedAt: Long = 0
)
