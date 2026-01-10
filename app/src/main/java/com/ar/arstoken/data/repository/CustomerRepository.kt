package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.CustomerEntity
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {

    fun getCustomers(): Flow<List<CustomerEntity>>

    suspend fun addCustomer(customer: CustomerEntity)
    suspend fun updateCustomerPhone(
        customerId: Int,
        phone: String
    )

}
