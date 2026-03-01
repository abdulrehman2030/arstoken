package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.ReportRepository
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.ItemSalesRow
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.repository.SettingsRepository
import com.ar.arstoken.util.formatReceipt
import com.ar.arstoken.util.endOfToday
import com.ar.arstoken.util.startOfToday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.ar.arstoken.util.startOfWeek
import com.ar.arstoken.util.startOfMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit


class ItemSalesViewModel(
    private val reportRepository: ReportRepository,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    enum class ItemSummarySort {
        AMOUNT_DESC,
        QTY_DESC,
        NAME_ASC
    }

    private val fromDate = MutableStateFlow(startOfToday())
    private val toDate = MutableStateFlow(endOfToday())
    private val itemSort = MutableStateFlow(ItemSummarySort.AMOUNT_DESC)

    val fromDateMillis = fromDate
    val toDateMillis = toDate

    @OptIn(ExperimentalCoroutinesApi::class)
    val sales = combine(fromDate, toDate) { from, to ->
        from to to
    }.flatMapLatest { (from, to) ->
        reportRepository.getSales(from, to).map { list ->
            list.sortedByDescending { it.id }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val itemSales = combine(fromDate, toDate, itemSort) { from, to, sort ->
        Triple(from, to, sort)
    }.flatMapLatest { (from, to, sort) ->
        reportRepository.getItemSales(from, to).map { list ->
            sortItems(list, sort)
        }
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

    fun setLast7Days() {
        val now = System.currentTimeMillis()
        fromDate.value = startOfDayMillis(now - TimeUnit.DAYS.toMillis(6))
        toDate.value = endOfToday()
    }

    fun setLast30Days() {
        val now = System.currentTimeMillis()
        fromDate.value = startOfDayMillis(now - TimeUnit.DAYS.toMillis(29))
        toDate.value = endOfToday()
    }

    fun setDateRange(from: Long, to: Long) {
        if (from <= to) {
            fromDate.value = from
            toDate.value = to
        }
    }

    fun setItemSummarySort(sort: ItemSummarySort) {
        itemSort.value = sort
    }

    val itemSummarySort = itemSort

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

    private fun sortItems(
        rows: List<ItemSalesRow>,
        sort: ItemSummarySort
    ): List<ItemSalesRow> {
        return when (sort) {
            ItemSummarySort.AMOUNT_DESC -> rows.sortedByDescending { it.totalAmount }
            ItemSummarySort.QTY_DESC -> rows.sortedByDescending { it.totalQty }
            ItemSummarySort.NAME_ASC -> rows.sortedBy { it.itemName.lowercase() }
        }
    }

    private fun startOfDayMillis(time: Long): Long {
        val day = TimeUnit.DAYS.toMillis(1)
        return (time / day) * day
    }
}
