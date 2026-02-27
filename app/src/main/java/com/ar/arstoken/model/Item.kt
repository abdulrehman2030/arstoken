package com.ar.arstoken.model

data class Item(
    val id: Int,
    val cloudId: String,
    val name: String,
    val price: Int,
    val category: String? = null
)
