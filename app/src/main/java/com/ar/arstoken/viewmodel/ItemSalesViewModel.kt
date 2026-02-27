package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.ReportRepository
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.repository.SettingsRepository
import com.ar.arstoken.util.formatReceipt
import com.ar.arstoken.util.endOfToday
import com.ar.arstoken.util.startOfToday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.ar.arstoken.util.startOfWeek
import com.ar.arstoken.util.startOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first


class ItemSalesViewModel(
    private val reportRepository: ReportRepository,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val fromDate = MutableStateFlow(startOfToday())
    private val toDate = MutableStateFlow(endOfToday())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sales = combine(fromDate, toDate) { from, to ->
        from to to
    }.flatMapLatest { (from, to) ->
        reportRepository.getSales(from, to)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun setToday() {
        fromDate.value = startOfToday()
        toDate.value = endOfToday()
    }
    fun setThisWeek() {
        fromDate.value = startOfWeek()
        toDate.value = endOfToday()
    }

    fun setThisMonth() {
        fromDate.value = startOfMonth()
        toDate.value = endOfToday()
    }

    suspend fun buildReceipt(
        saleId: Int,
        businessName: String?,
        businessPhone: String?
    ): String? =
        withContext(Dispatchers.IO) {
            val sale = db.saleDao().getSaleById(saleId).first() ?: return@withContext null
            val items = db.saleItemDao().getItemsForSale(saleId).first()
            val settings = settingsRepository.getOnce()
            formatReceipt(
                settings = settings,
                businessNameOverride = businessName,
                businessPhoneOverride = businessPhone,
                sale = sale,
                items = items,
                headerNote = "TOKEN COPY"
            )
        }
}
