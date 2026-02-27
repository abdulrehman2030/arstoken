package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_ledger")
data class CreditLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cloudId: String = "",

    val customerId: Int,
    val customerCloudId: String? = null,
    val customerName: String,

    val saleId: String,
    val saleCloudId: String? = null,

    val timestamp: Long,

    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,

    val synced: Boolean = false,   // 🔑 cloud-ready
    val updatedAt: Long = 0
)
