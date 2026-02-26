package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,

    val customerId: Int?,
    val customerName: String?,

    val saleType: String,

    val totalAmount: Int,
    val paidAmount: Int,
    val dueAmount: Int,

    val synced: Boolean = false   // 🔑 cloud-ready
)
