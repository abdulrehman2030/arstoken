//package com.ar.arstoken.data
//
//import com.ar.arstoken.data.db.SaleEntity
//import com.ar.arstoken.data.db.SaleItemEntity
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//
//class FakeSaleRepository : SaleRepository {
//
//    override suspend fun saveSale(sale: SaleEntity): Long {
//        // Fake ID for testing / previews
//        return 1L
//    }
//
//    override suspend fun saveSaleItems(items: List<SaleItemEntity>) {
//        // No-op for fake repository
//    }
//
//    override fun getSalesForCustomer(customerId: Int): Flow<List<SaleEntity>> {
//        TODO("Not yet implemented")
//    }
//}
//
