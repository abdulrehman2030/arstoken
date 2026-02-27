package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {

    @Query("DELETE FROM sale_items")
    suspend fun deleteAll()

    @Query(
        """
        SELECT 
            itemId,
            itemName,
            SUM(quantity) AS totalQty,
            SUM(totalPrice) AS totalAmount
        FROM sale_items
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY itemId, itemName
        ORDER BY totalAmount DESC
        """
    )
    fun getItemSales(
        from: Long,
        to: Long
    ): Flow<List<ItemSalesRow>>

    @Insert
    suspend fun insertAll(items: List<SaleItemEntity>)

    @Query("""
        SELECT *
        FROM sale_items
        WHERE saleId = :saleId
        ORDER BY id ASC
    """)
    fun getItemsForSale(saleId: Int): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllOnce(): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): SaleItemEntity?

    @Query("""
        UPDATE sale_items
        SET saleId = :saleId,
            saleCloudId = :saleCloudId,
            itemId = :itemId,
            itemCloudId = :itemCloudId,
            itemName = :itemName,
            quantity = :quantity,
            unitPrice = :unitPrice,
            totalPrice = :totalPrice,
            timestamp = :timestamp,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        saleId: Int,
        saleCloudId: String,
        itemId: Int,
        itemCloudId: String,
        itemName: String,
        quantity: Double,
        unitPrice: Int,
        totalPrice: Double,
        timestamp: Long,
        updatedAt: Long
    )

    @Query("""
        UPDATE sale_items
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)
}
