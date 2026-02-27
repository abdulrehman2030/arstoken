package com.ar.arstoken.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreSettingsDao {

    @Query("DELETE FROM store_settings")
    suspend fun deleteAll()

    @Query("SELECT * FROM store_settings WHERE id = 1")
    fun observeSettings(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1")
    suspend fun getOnce(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: StoreSettingsEntity)
}
