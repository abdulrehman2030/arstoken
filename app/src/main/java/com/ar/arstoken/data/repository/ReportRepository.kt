package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.ItemSalesRow
import com.ar.arstoken.model.ReportSummary
import kotlinx.coroutines.flow.Flow

interface ReportRepository {

    suspend fun getReport(from: Long, to: Long): ReportSummary

    fun getItemSales(from: Long, to: Long): Flow<List<ItemSalesRow>>
}
