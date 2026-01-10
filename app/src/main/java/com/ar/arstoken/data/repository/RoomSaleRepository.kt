package com.ar.arstoken.data.repository

import com.ar.arstoken.data.SaleRepository
import com.ar.arstoken.data.db.*
import com.ar.arstoken.model.Sale
import com.ar.arstoken.model.SaleItem
import com.ar.arstoken.model.SaleType
import kotlinx.coroutines.flow.Flow


class RoomSaleRepository(
    private val db: AppDatabase
) : SaleRepository {

    override suspend fun saveSale(sale: SaleEntity): Long {
        return db.saleDao().insert(sale)   // returns saleId
    }

    override suspend fun saveSaleItems(items: List<SaleItemEntity>) {
        db.saleItemDao().insertAll(items)
    }


    override fun getSalesForCustomer(
        customerId: Int
    ): Flow<List<SaleEntity>> {
        return db.saleDao().getSalesForCustomer(customerId)
    }

    override fun getCustomerDues(): Flow<List<CustomerDueRow>> {
        return db.saleDao().getCustomerDues()
    }

}