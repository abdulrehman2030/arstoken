package com.ar.arstoken.data

import com.ar.arstoken.data.db.CustomerDueRow
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.model.Sale
import kotlinx.coroutines.flow.Flow

interface SaleRepository {

    suspend fun saveSale(sale: SaleEntity): Long
    suspend fun saveSaleItems(items: List<SaleItemEntity>)

    fun getSalesForCustomer(customerId: Int): Flow<List<SaleEntity>>
    fun getCustomerDues(): Flow<List<CustomerDueRow>>

}