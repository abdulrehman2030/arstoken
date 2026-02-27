package com.ar.arstoken.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.db.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class BillDetailViewModel(
    db: AppDatabase,
    saleId: Int
) : ViewModel() {

    val sale = db.saleDao().getSaleById(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val items = db.saleItemDao().getItemsForSale(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
