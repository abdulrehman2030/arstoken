package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.SaleRepository
import com.ar.arstoken.data.db.CreditLedgerEntity
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.model.SaleType
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerLedgerViewModel(
    private val customerId: Int,
    private val customerName: String,
    private val customerCloudId: String?,
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
        if (amount <= 0.0 || amount > currentDue) return

        val now = nowMs()
        val saleCloudId = newCloudId()
        val paymentSale = SaleEntity(
            cloudId = saleCloudId,
            timestamp = System.currentTimeMillis(),
            customerId = customerId,
            customerCloudId = customerCloudId,
            customerName = customerName,
            saleType = SaleType.PAYMENT.name,
            totalAmount = 0.0,
            paidAmount = amount,
            dueAmount = -amount,
            updatedAt = now
        )

        viewModelScope.launch {
            val paymentSaleId = saleRepository.saveSale(paymentSale)
            saleRepository.saveCreditLedgerEntry(
                CreditLedgerEntity(
                    cloudId = newCloudId(),
                    customerId = customerId,
                    customerCloudId = customerCloudId,
                    customerName = customerName,
                    saleId = paymentSaleId.toString(),
                    saleCloudId = saleCloudId,
                    timestamp = paymentSale.timestamp,
                    totalAmount = paymentSale.totalAmount,
                    paidAmount = paymentSale.paidAmount,
                    dueAmount = paymentSale.dueAmount,
                    updatedAt = now
                )
            )
        }
    }

    fun getCurrentDue(): Double {
        return sales.value.sumOf { it.totalAmount - it.paidAmount }
    }

}
