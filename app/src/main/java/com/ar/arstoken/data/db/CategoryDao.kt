package com.ar.arstoken.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY name")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Query(
        """
        UPDATE categories
        SET isActive = 0
        WHERE id = :categoryId
        """
    )
    suspend fun softDelete(categoryId: Int)
}
