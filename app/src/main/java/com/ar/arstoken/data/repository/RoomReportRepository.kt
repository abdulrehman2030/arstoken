package com.ar.arstoken.data.repository

import com.ar.arstoken.data.ReportRepository
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.model.ReportSummary
import kotlinx.coroutines.flow.Flow

class RoomReportRepository(
    private val db: AppDatabase
) : ReportRepository {

    private val saleDao = db.saleDao()
    private val ledgerDao = db.creditLedgerDao()

    suspend fun getReport(
        from: Long,
        to: Long
    ): ReportSummary {

        val totalSales = saleDao.getTotalSales(from, to) ?: 0
        val totalCash = saleDao.getTotalCash(from, to) ?: 0
        val totalCredit = saleDao.getTotalCredit(from, to) ?: 0
        val totalDue = ledgerDao.getOverallDue() ?: 0

        return ReportSummary(
            totalSales = totalSales,
            totalCash = totalCash,
            totalCredit = totalCredit,
            totalDue = totalDue
        )
    }

    override fun getSales(
        from: Long,
        to: Long
    ): Flow<List<SaleEntity>> {
        return saleDao.getSalesBetween(from, to)
    }
}
