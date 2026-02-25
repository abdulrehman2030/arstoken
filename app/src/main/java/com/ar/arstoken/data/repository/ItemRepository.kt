package com.ar.arstoken.data.repository

import com.ar.arstoken.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow

interface ItemRepository {

    fun getItems(): Flow<List<ItemEntity>>

    suspend fun addItem(name: String, price: Double)

    suspend fun updatePrice(itemId: Int, price: Double)

    suspend fun deleteItem(itemId: Int)
}
