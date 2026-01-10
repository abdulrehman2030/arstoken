package com.ar.arstoken.model

import java.util.UUID

enum class SaleType {
    CASH,
    CREDIT,
    PARTIAL,
    PAYMENT
}

data class SaleItem(
    val itemId: Int,
    val name: String,
    val unitPrice: Double,
    val quantity: Double,
    val totalPrice: Double
)

data class Sale(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),

    val customerId: Int? = null,
    val customerName: String? = null,

    val saleType: SaleType,

    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,

    val items: List<SaleItem>
)
