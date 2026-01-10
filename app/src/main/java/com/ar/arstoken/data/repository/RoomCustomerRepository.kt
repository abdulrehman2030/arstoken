package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.CustomerEntity
import com.ar.arstoken.data.repository.CustomerRepository

class RoomCustomerRepository(
    private val db: AppDatabase
) : CustomerRepository {

    override fun getCustomers() =
        db.customerDao().getAllCustomers()

    override suspend fun addCustomer(customer: CustomerEntity) {
        db.customerDao().insert(customer)
    }

    override suspend fun updateCustomerPhone(
        customerId: Int,
        phone: String
    ) {
        db.customerDao().updatePhone(customerId, phone)
    }
}
