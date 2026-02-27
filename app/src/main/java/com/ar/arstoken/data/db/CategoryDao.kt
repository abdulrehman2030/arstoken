package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE cloudId = :cloudId LIMIT 1")
    suspend fun findByCloudId(cloudId: String): CategoryEntity?

    @Query("""
        UPDATE categories
        SET cloudId = :cloudId,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateCloudId(id: Int, cloudId: String, updatedAt: Long)

    @Query("""
        UPDATE categories
        SET name = :name,
            isActive = :isActive,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateFromCloud(
        id: Int,
        name: String,
        isActive: Boolean,
        updatedAt: Long
    )

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Query(
        """
        UPDATE categories
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE id = :categoryId
        """
    )
    suspend fun softDelete(categoryId: Int, updatedAt: Long)
}
