package com.ar.arstoken.data

import com.ar.arstoken.data.db.ItemSalesRow
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getItemSales(from: Long, to: Long): Flow<List<ItemSalesRow>>
}
