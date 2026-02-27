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

    @Query("SELECT * FROM sales")
    suspend fun getAllOnce(): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): SaleEntity?

    @Query("""
        UPDATE sales
        SET timestamp = :timestamp,
            customerId = :customerId,
            customerCloudId = :customerCloudId,
            customerName = :customerName,
            saleType = :saleType,
            totalAmount = :totalAmount,
            paidAmount = :paidAmount,
            dueAmount = :dueAmount,
            synced = :synced,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        timestamp: Long,
        customerId: Int?,
        customerCloudId: String?,
        customerName: String?,
        saleType: String,
        totalAmount: Int,
        paidAmount: Int,
        dueAmount: Int,
        synced: Boolean,
        updatedAt: Long
    )

    @Query("""
        UPDATE sales
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)

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
    SELECT *
    FROM sales
    WHERE timestamp BETWEEN :from AND :to
    ORDER BY id DESC
""")
    fun getSalesBetween(from: Long, to: Long): Flow<List<SaleEntity>>

    @Query("""
    SELECT *
    FROM sales
    WHERE id = :saleId
    LIMIT 1
""")
    fun getSaleById(saleId: Int): Flow<SaleEntity?>

    @Query("""
    SELECT *
    FROM sales
    WHERE id = :saleId
    LIMIT 1
""")
    suspend fun getSaleByIdOnce(saleId: Int): SaleEntity?

    @Query("""
    SELECT customerId, SUM(totalAmount - paidAmount) AS due
    FROM sales
    WHERE customerId != 0
    GROUP BY customerId
""")
    fun getCustomerDues(): Flow<List<CustomerDueRow>>


}
