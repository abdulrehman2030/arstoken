package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow

class RoomItemRepository(
    db: AppDatabase
) : ItemRepository {

    private val dao = db.itemDao()

    override fun getItems(): Flow<List<ItemEntity>> =
        dao.getActiveItems()

    override suspend fun addItem(name: String, price: Double) {
        dao.insert(
            ItemEntity(
                name = name,
                price = price
            )
        )
    }

    override suspend fun updatePrice(itemId: Int, price: Double) {
        dao.updatePrice(itemId, price)
    }

    override suspend fun deleteItem(itemId: Int) {
        dao.softDelete(itemId)
    }
}
