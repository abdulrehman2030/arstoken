package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name")
    fun getActiveItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun getAllOnce(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): ItemEntity?

    @Query("""
        UPDATE items
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)

    @Query("""
        UPDATE items
        SET name = :name,
            price = :price,
            category = :category,
            isActive = :isActive,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        name: String,
        price: Int,
        category: String?,
        isActive: Boolean,
        updatedAt: Long
    )

    @Insert
    suspend fun insert(item: ItemEntity)

    @Query(
        """
        UPDATE items
        SET category = :category,
            updatedAt = :updatedAt
        WHERE id = :itemId
        """
    )
    suspend fun updateCategory(itemId: Int, category: String?, updatedAt: Long)

    @Query("""
        UPDATE items
        SET price = :price,
            updatedAt = :updatedAt
        WHERE id = :itemId
    """)
    suspend fun updatePrice(itemId: Int, price: Int, updatedAt: Long)

    @Query("""
        UPDATE items
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE id = :itemId
    """)
    suspend fun softDelete(itemId: Int, updatedAt: Long)
}
