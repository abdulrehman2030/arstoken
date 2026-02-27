package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.CustomerEntity
import com.ar.arstoken.data.repository.CustomerRepository
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs

class RoomCustomerRepository(
    private val db: AppDatabase
) : CustomerRepository {

    override fun getCustomers() =
        db.customerDao().getAllCustomers()

    override suspend fun addCustomer(customer: CustomerEntity) {
        val withSync = if (customer.cloudId.isBlank()) {
            customer.copy(
                cloudId = newCloudId(),
                updatedAt = nowMs()
            )
        } else {
            customer.copy(updatedAt = nowMs())
        }
        db.customerDao().insert(withSync)
    }

    override suspend fun updateCustomerPhone(
        customerId: Int,
        phone: String
    ) {
        db.customerDao().updatePhone(customerId, phone, nowMs())
    }
}
