package com.ar.arstoken.model

data class Customer(
    val id: Int,
    val cloudId: String,
    val name: String,
    val phone: String,
    val creditBalance: Double = 0.0
)
