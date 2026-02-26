package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.StoreSettingsDao
import com.ar.arstoken.data.db.StoreSettingsEntity
import kotlinx.coroutines.flow.first

class RoomSettingsRepository(
    private val dao: StoreSettingsDao
) : SettingsRepository {

    override fun observe() = dao.observeSettings()

    override suspend fun save(settings: StoreSettingsEntity) {
        dao.save(settings.copy(id = 1))
    }

    override suspend fun getOnce(): StoreSettingsEntity {
        return dao.observeSettings().first()
            ?: StoreSettingsEntity(
                storeName = "My Store",
                phone = ""
            )
    }

}
