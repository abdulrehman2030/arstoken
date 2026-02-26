package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: SaleEntity)

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    suspend fun insertFullSale(
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ) {
        insertSale(sale)
        insertItems(items)
    }
    @Query("""
    SELECT 
        SUM(totalAmount) 
    FROM sales
    WHERE timestamp BETWEEN :from AND :to
""")
    suspend fun getTotalSales(from: Long, to: Long): Int?

    @Query("""
    SELECT 
        SUM(paidAmount) 
    FROM sales
    WHERE saleType = 'CASH'
    AND timestamp BETWEEN :from AND :to
""")
    suspend fun getTotalCash(from: Long, to: Long): Int?

    @Query("""
    SELECT 
        SUM(dueAmount) 
    FROM sales
    WHERE saleType IN ('CREDIT','PARTIAL')
    AND timestamp BETWEEN :from AND :to
""")
    suspend fun getTotalCredit(from: Long, to: Long): Int?

    @Insert
    suspend fun insert(sale: SaleEntity): Long

//    @Query("""
//    SELECT *
//    FROM sales
//    WHERE customerId = :customerId
//    ORDER BY timestamp ASC
//""")
//    fun getSalesForCustomer(customerId: Int): Flow<List<SaleEntity>>

    @Query("""
    SELECT *
    FROM sales
    WHERE customerId = :customerId
    ORDER BY timestamp DESC
""")
    fun getSalesForCustomer(customerId: Int): Flow<List<SaleEntity>>

    @Query("""
    SELECT customerId, SUM(totalAmount - paidAmount) AS due
    FROM sales
    WHERE customerId != 0
    GROUP BY customerId
""")
    fun getCustomerDues(): Flow<List<CustomerDueRow>>


}
