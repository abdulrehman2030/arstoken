package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ar.arstoken.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    var draftItemName by mutableStateOf("")
        private set
    var draftItemPrice by mutableStateOf("")
        private set
    var draftItemCategory by mutableStateOf<String?>(null)
        private set

    val items = repository.getItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val categories = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun updateDraftName(value: String) {
        draftItemName = value
    }

    fun updateDraftPrice(value: String) {
        draftItemPrice = value
    }

    fun updateDraftCategory(value: String?) {
        draftItemCategory = value
    }

    fun addItem() {
        val name = draftItemName
        val price = draftItemPrice.toIntOrNull() ?: 0
        val category = draftItemCategory
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.addItem(name, price, category)
            draftItemName = ""
            draftItemPrice = ""
            draftItemCategory = null
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun assignCategory(itemId: Int, category: String?) {
        viewModelScope.launch {
            repository.assignCategory(itemId, category)
        }
    }

    fun updatePrice(itemId: Int, price: Int) {
        viewModelScope.launch {
            repository.updatePrice(itemId, price)
        }
    }

    fun deleteItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
        }
    }
}
