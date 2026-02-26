package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    fun getItems(): Flow<List<ItemEntity>>

    fun getCategories(): Flow<List<String>>

    suspend fun addItem(name: String, price: Int, category: String?)

    suspend fun addCategory(name: String)

    suspend fun assignCategory(itemId: Int, category: String?)

    suspend fun updatePrice(itemId: Int, price: Int)

    suspend fun deleteItem(itemId: Int)
}
