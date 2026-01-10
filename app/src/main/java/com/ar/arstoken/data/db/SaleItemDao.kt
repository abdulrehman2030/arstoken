package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {

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
}
