package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_ledger")
data class CreditLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val customerId: Int,
    val customerName: String,

    val saleId: String,

    val timestamp: Long,

    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,

    val synced: Boolean = false   // 🔑 cloud-ready
)
