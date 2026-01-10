package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.ReportRepository
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


class ItemSalesViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val fromDate = MutableStateFlow(startOfToday())
    private val toDate = MutableStateFlow(endOfToday())

    @OptIn(ExperimentalCoroutinesApi::class)
    val items = combine(fromDate, toDate) { from, to ->
        from to to
    }.flatMapLatest { (from, to) ->
        reportRepository.getItemSales(from, to)
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

}
