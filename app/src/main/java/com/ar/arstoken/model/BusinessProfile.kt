package com.ar.arstoken.model

data class BusinessProfile(
    val businessName: String = "",
    val logoUrl: String? = null,
    val phone: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
