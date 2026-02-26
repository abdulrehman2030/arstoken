package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name")
    fun getActiveItems(): Flow<List<ItemEntity>>

    @Insert
    suspend fun insert(item: ItemEntity)

    @Query(
        """
        UPDATE items
        SET category = :category
        WHERE id = :itemId
        """
    )
    suspend fun updateCategory(itemId: Int, category: String?)

    @Query("""
        UPDATE items
        SET price = :price
        WHERE id = :itemId
    """)
    suspend fun updatePrice(itemId: Int, price: Int)

    @Query("""
        UPDATE items
        SET isActive = 0
        WHERE id = :itemId
    """)
    suspend fun softDelete(itemId: Int)
}
