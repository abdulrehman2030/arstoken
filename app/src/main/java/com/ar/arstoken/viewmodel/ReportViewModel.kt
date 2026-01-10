package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.repository.RoomReportRepository
import com.ar.arstoken.model.ReportSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.ar.arstoken.util.DateUtils


class ReportViewModel(
    private val repository: RoomReportRepository
) : ViewModel() {

    private val _report = MutableStateFlow<ReportSummary?>(null)
    val report: StateFlow<ReportSummary?> = _report

    fun loadReport(from: Long, to: Long) {
        viewModelScope.launch {
            _report.value = repository.getReport(from, to)
        }
    }

    fun loadTodayReport() {
        val (from, to) = DateUtils.todayRange()
        loadReport(from, to)
    }

}
