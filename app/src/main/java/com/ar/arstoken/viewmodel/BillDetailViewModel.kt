package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.CreditLedgerEntity
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class BillDetailViewModel(
    private val db: AppDatabase,
    private val saleId: Int
) : ViewModel() {

    val sale = db.saleDao().getSaleById(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items = db.saleItemDao().getItemsForSale(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun deleteBill(reason: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedReason = reason.trim()
        if (trimmedReason.isBlank()) return@withContext false

        val existing = db.saleDao().getSaleByIdOnce(saleId) ?: return@withContext false
        if (existing.isDeleted) return@withContext false

        val now = nowMs()
        val safeUpdatedAt = maxOf(now, existing.updatedAt + 1L)
        db.saleDao().softDeleteSale(
            saleId = saleId,
            reason = trimmedReason,
            deletedAt = now,
            updatedAt = safeUpdatedAt
        )

        if ((existing.customerId ?: 0) > 0 && existing.dueAmount > 0.0) {
            db.creditLedgerDao().insert(
                CreditLedgerEntity(
                    cloudId = newCloudId(),
                    customerId = existing.customerId ?: 0,
                    customerCloudId = existing.customerCloudId,
                    customerName = existing.customerName ?: "Customer",
                    saleId = existing.id.toString(),
                    saleCloudId = existing.cloudId,
                    timestamp = now,
                    totalAmount = 0.0,
                    paidAmount = existing.dueAmount,
                    dueAmount = -existing.dueAmount,
                    updatedAt = now
                )
            )
        }
        true
    }
}
