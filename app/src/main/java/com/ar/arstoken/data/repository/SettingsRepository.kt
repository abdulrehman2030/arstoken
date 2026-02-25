package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.StoreSettingsEntity
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<StoreSettingsEntity?>
    suspend fun save(name: String, phone: String)
    suspend fun getOnce(): StoreSettingsEntity

}
