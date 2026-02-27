package com.ar.arstoken.data

import com.ar.arstoken.data.db.SaleEntity
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getSales(from: Long, to: Long): Flow<List<SaleEntity>>
}
