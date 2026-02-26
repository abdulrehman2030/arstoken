package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.repository.ItemRepository
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    fun addCategory(name: String, onDone: (String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            repository.addCategory(trimmed)
            onDone(trimmed)
        }
    }
}
