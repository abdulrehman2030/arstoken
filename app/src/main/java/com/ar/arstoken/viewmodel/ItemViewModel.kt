package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    val items = repository.getItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun addItem(name: String, price: Double) {
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.addItem(name, price)
        }
    }

    fun updatePrice(itemId: Int, price: Double) {
        viewModelScope.launch {
            repository.updatePrice(itemId, price)
        }
    }
}
