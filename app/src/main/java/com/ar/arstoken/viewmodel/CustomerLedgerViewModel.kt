package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.SaleRepository
import com.ar.arstoken.data.db.CreditLedgerEntity
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.model.SaleType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerLedgerViewModel(
    private val customerId: Int,
    private val customerName: String,
    private val saleRepository: SaleRepository
) : ViewModel() {

    val sales = saleRepository
        .getSalesForCustomer(customerId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun receivePayment(amount: Double) {
        val currentDue = getCurrentDue()
        if (amount <= 0 || amount > currentDue) return

        val paymentSale = SaleEntity(
            timestamp = System.currentTimeMillis(),
            customerId = customerId,
            customerName = customerName,
            saleType = SaleType.PAYMENT.name,
            totalAmount = 0.0,
            paidAmount = amount,
            dueAmount = -amount
        )

        viewModelScope.launch {
            val paymentSaleId = saleRepository.saveSale(paymentSale)
            saleRepository.saveCreditLedgerEntry(
                CreditLedgerEntity(
                    customerId = customerId,
                    customerName = customerName,
                    saleId = paymentSaleId.toString(),
                    timestamp = paymentSale.timestamp,
                    totalAmount = paymentSale.totalAmount,
                    paidAmount = paymentSale.paidAmount,
                    dueAmount = paymentSale.dueAmount
                )
            )
        }
    }

    fun getCurrentDue(): Double {
        return sales.value.sumOf { it.totalAmount - it.paidAmount }
    }

}
