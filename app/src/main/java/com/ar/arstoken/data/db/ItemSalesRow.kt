package com.ar.arstoken.data.db

data class ItemSalesRow(
    val itemId: Int,
    val itemName: String,
    val itemCategory: String?,
    val totalQty: Double,
    val totalAmount: Double
)
