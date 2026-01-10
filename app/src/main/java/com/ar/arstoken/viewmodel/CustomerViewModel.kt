package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.db.CustomerEntity
import com.ar.arstoken.data.repository.CustomerRepository
import com.ar.arstoken.data.SaleRepository
import com.ar.arstoken.model.Customer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    /**
     * Customers list with dynamically calculated due
     * (derived from sales, NOT stored)
     */
    val customersWithDue = combine(
        customerRepository.getCustomers(),
        saleRepository.getCustomerDues()
    ) { customers, dues ->

        val dueMap = dues.associateBy(
            { it.customerId },
            { it.due }
        )

        customers.map { customer ->
            Customer(
                id = customer.id,
                name = customer.name,
                phone = customer.phone,
                creditBalance = dueMap[customer.id] ?: 0.0
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun addCustomer(name: String, phone: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            customerRepository.addCustomer(
                CustomerEntity(
                    name = name,
                    phone = phone
                )
            )
        }
    }
    fun updateCustomerPhone(customerId: Int, phone: String) {
        if (phone.isBlank()) return

        viewModelScope.launch {
            customerRepository.updateCustomerPhone(
                customerId = customerId,
                phone = phone
            )
        }
    }
}
