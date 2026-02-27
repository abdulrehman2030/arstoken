package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.CategoryEntity
import com.ar.arstoken.data.db.ItemEntity
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomItemRepository(
    db: AppDatabase
) : ItemRepository {

    private val dao = db.itemDao()
    private val categoryDao = db.categoryDao()

    override fun getItems(): Flow<List<ItemEntity>> =
        dao.getActiveItems()

    override fun getCategories(): Flow<List<String>> {
        val fromCategoryTable = categoryDao.getActiveCategories().map { list ->
            list.map { it.name.trim() }
                .filter { it.isNotBlank() }
        }
        val fromItemTable = dao.getActiveItems().map { list ->
            list.mapNotNull { it.category?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        return combine(fromCategoryTable, fromItemTable) { a, b ->
            (a + b).distinct().sorted()
        }
    }

    override suspend fun addItem(name: String, price: Int, category: String?) {
        dao.insert(
            ItemEntity(
                cloudId = newCloudId(),
                name = name,
                price = price,
                category = category?.trim()?.takeIf { it.isNotBlank() },
                updatedAt = nowMs()
            )
        )
    }

    override suspend fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        categoryDao.insert(
            CategoryEntity(
                cloudId = newCloudId(),
                name = trimmed,
                updatedAt = nowMs()
            )
        )
    }

    override suspend fun assignCategory(itemId: Int, category: String?) {
        dao.updateCategory(itemId, category?.trim()?.takeIf { it.isNotBlank() }, nowMs())
    }

    override suspend fun updatePrice(itemId: Int, price: Int) {
        dao.updatePrice(itemId, price, nowMs())
    }

    override suspend fun deleteItem(itemId: Int) {
        dao.softDelete(itemId, nowMs())
    }
}
