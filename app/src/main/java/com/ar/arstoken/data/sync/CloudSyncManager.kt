package com.ar.arstoken.data.sync

import com.ar.arstoken.data.db.*
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CloudSyncManager(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore
) {

    suspend fun refreshFromCloudOnLogin(uid: String) = withContext(Dispatchers.IO) {
        clearLocalUserData()
        syncAll(uid)
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        clearLocalUserData()
    }

    suspend fun syncAll(uid: String) = withContext(Dispatchers.IO) {
        syncCustomers(uid)
        syncItems(uid)
        syncCategories(uid)
        syncSales(uid)
        syncSaleItems(uid)
        syncCreditLedger(uid)
        syncSettings(uid)
    }

    private fun base(uid: String) =
        firestore.collection("users").document(uid)

    private suspend fun clearLocalUserData() {
        // Delete child tables first to avoid FK/order issues.
        db.creditLedgerDao().deleteAll()
        db.saleItemDao().deleteAll()
        db.saleDao().deleteAll()
        db.customerDao().deleteAll()
        db.itemDao().deleteAll()
        db.categoryDao().deleteAll()
        db.storeSettingsDao().deleteAll()
    }

    private suspend fun syncCustomers(uid: String) {
        val dao = db.customerDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("customers").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(CustomerRemote::class.java) ?: CustomerRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val resolvedCloudId = resolveCustomerCloudId(item, remoteById.values)
            if (item.cloudId.isBlank() && resolvedCloudId != null) {
                dao.updateCloudId(item.id, resolvedCloudId, maxOf(item.updatedAt, nowMs()))
            }
            val cloudId = resolvedCloudId ?: item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank() && resolvedCloudId == null) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null || item.updatedAt > remote.updatedAt) {
                base(uid).collection("customers")
                    .document(cloudId)
                    .set(item.toRemote(cloudId), SetOptions.merge())
                    .await()
            } else if (remote.updatedAt > item.updatedAt) {
                dao.updateFromCloud(
                    id = item.id,
                    name = remote.name,
                    phone = remote.phone,
                    creditBalance = remote.creditBalance,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                dao.insert(remote.toEntity())
            }
        }
    }

    private suspend fun syncItems(uid: String) {
        val dao = db.itemDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("items").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(ItemRemote::class.java) ?: ItemRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val resolvedCloudId = resolveItemCloudId(item, remoteById.values)
            if (item.cloudId.isBlank() && resolvedCloudId != null) {
                dao.updateCloudId(item.id, resolvedCloudId, maxOf(item.updatedAt, nowMs()))
            }
            val cloudId = resolvedCloudId ?: item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank() && resolvedCloudId == null) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null) {
                if (item.isActive) {
                    base(uid).collection("items")
                        .document(cloudId)
                        .set(item.toRemote(cloudId), SetOptions.merge())
                        .await()
                }
            } else if (item.updatedAt > remote.updatedAt) {
                if (item.isActive) {
                    base(uid).collection("items")
                        .document(cloudId)
                        .set(item.toRemote(cloudId), SetOptions.merge())
                        .await()
                } else {
                    base(uid).collection("items")
                        .document(cloudId)
                        .delete()
                        .await()
                }
            } else if (remote.updatedAt > item.updatedAt) {
                dao.updateFromCloud(
                    id = item.id,
                    name = remote.name,
                    price = remote.price,
                    category = remote.category,
                    isActive = remote.isActive,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                if (remote.isActive) {
                    dao.insert(remote.toEntity())
                }
            }
        }
    }

    private suspend fun syncCategories(uid: String) {
        val dao = db.categoryDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("categories").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(CategoryRemote::class.java) ?: CategoryRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val resolvedCloudId = resolveCategoryCloudId(item, remoteById.values)
            if (item.cloudId.isBlank() && resolvedCloudId != null) {
                dao.updateCloudId(item.id, resolvedCloudId, maxOf(item.updatedAt, nowMs()))
            }
            val cloudId = resolvedCloudId ?: item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank() && resolvedCloudId == null) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null) {
                if (item.isActive) {
                    base(uid).collection("categories")
                        .document(cloudId)
                        .set(item.toRemote(cloudId), SetOptions.merge())
                        .await()
                }
            } else if (item.updatedAt > remote.updatedAt) {
                if (item.isActive) {
                    base(uid).collection("categories")
                        .document(cloudId)
                        .set(item.toRemote(cloudId), SetOptions.merge())
                        .await()
                } else {
                    base(uid).collection("categories")
                        .document(cloudId)
                        .delete()
                        .await()
                }
            } else if (remote.updatedAt > item.updatedAt) {
                dao.updateFromCloud(
                    id = item.id,
                    name = remote.name,
                    isActive = remote.isActive,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                if (remote.isActive) {
                    dao.insert(remote.toEntity())
                }
            }
        }
    }

    private suspend fun syncSales(uid: String) {
        val dao = db.saleDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("sales").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(SaleRemote::class.java) ?: SaleRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val cloudId = item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank()) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null || item.updatedAt > remote.updatedAt) {
                base(uid).collection("sales")
                    .document(cloudId)
                    .set(item.toRemote(cloudId), SetOptions.merge())
                    .await()
            } else if (remote.updatedAt > item.updatedAt) {
                val customerId = remote.customerCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.customerDao().findByCloudId(it)?.id }
                dao.updateFromCloud(
                    id = item.id,
                    timestamp = remote.timestamp,
                    customerId = customerId,
                    customerCloudId = remote.customerCloudId,
                    customerName = remote.customerName,
                    saleType = remote.saleType,
                    totalAmount = remote.totalAmount,
                    paidAmount = remote.paidAmount,
                    dueAmount = remote.dueAmount,
                    synced = true,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                val customerId = remote.customerCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.customerDao().findByCloudId(it)?.id }
                dao.insert(remote.toEntity(customerId))
            }
        }
    }

    private suspend fun syncSaleItems(uid: String) {
        val dao = db.saleItemDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("sale_items").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(SaleItemRemote::class.java) ?: SaleItemRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val cloudId = item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank()) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null || item.updatedAt > remote.updatedAt) {
                base(uid).collection("sale_items")
                    .document(cloudId)
                    .set(item.toRemote(cloudId), SetOptions.merge())
                    .await()
            } else if (remote.updatedAt > item.updatedAt) {
                val saleId = remote.saleCloudId
                    .takeIf { it.isNotBlank() }
                    ?.let { db.saleDao().findByCloudId(it)?.id }
                    ?: remote.billNumber.takeIf { it > 0 }
                        ?: item.saleId
                val itemId = remote.itemCloudId
                    .takeIf { it.isNotBlank() }
                    ?.let { db.itemDao().findByCloudId(it)?.id }
                    ?: item.itemId
                dao.updateFromCloud(
                    id = item.id,
                    saleId = saleId,
                    saleCloudId = remote.saleCloudId,
                    itemId = itemId,
                    itemCloudId = remote.itemCloudId,
                    itemName = remote.itemName,
                    quantity = remote.quantity,
                    unitPrice = remote.unitPrice,
                    totalPrice = remote.totalPrice,
                    timestamp = remote.timestamp,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                val saleId = remote.saleCloudId
                    .takeIf { it.isNotBlank() }
                    ?.let { db.saleDao().findByCloudId(it)?.id }
                    ?: remote.billNumber.takeIf { it > 0 } ?: 0
                val itemId = remote.itemCloudId
                    .takeIf { it.isNotBlank() }
                    ?.let { db.itemDao().findByCloudId(it)?.id }
                    ?: 0
                dao.insertAll(listOf(remote.toEntity(saleId, itemId)))
            }
        }
    }

    private suspend fun syncCreditLedger(uid: String) {
        val dao = db.creditLedgerDao()
        val local = dao.getAllOnce()
        val remoteSnap = base(uid).collection("credit_ledger").get().await()
        val remoteById = remoteSnap.documents.associate { doc ->
            val r = doc.toObject(CreditLedgerRemote::class.java) ?: CreditLedgerRemote()
            val cloudId = r.cloudId.ifBlank { doc.id }
            cloudId to r.copy(cloudId = cloudId)
        }

        local.forEach { item ->
            val cloudId = item.cloudId.ifBlank { newCloudId() }
            if (item.cloudId.isBlank()) {
                dao.updateCloudId(item.id, cloudId, nowMs())
            }
            val remote = remoteById[cloudId]
            if (remote == null || item.updatedAt > remote.updatedAt) {
                base(uid).collection("credit_ledger")
                    .document(cloudId)
                    .set(item.toRemote(cloudId), SetOptions.merge())
                    .await()
            } else if (remote.updatedAt > item.updatedAt) {
                val customerId = remote.customerCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.customerDao().findByCloudId(it)?.id }
                    ?: item.customerId
                val localSaleId = remote.saleCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.saleDao().findByCloudId(it)?.id?.toString() }
                    ?: remote.billNumber.takeIf { it > 0 }?.toString()
                    ?: remote.saleId
                dao.updateFromCloud(
                    id = item.id,
                    customerId = customerId,
                    customerCloudId = remote.customerCloudId,
                    customerName = remote.customerName,
                    saleId = localSaleId,
                    saleCloudId = remote.saleCloudId,
                    timestamp = remote.timestamp,
                    totalAmount = remote.totalAmount,
                    paidAmount = remote.paidAmount,
                    dueAmount = remote.dueAmount,
                    synced = true,
                    updatedAt = remote.updatedAt
                )
            }
        }

        remoteById.values.forEach { remote ->
            val localMatch = local.firstOrNull { it.cloudId == remote.cloudId }
            if (localMatch == null) {
                val customerId = remote.customerCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.customerDao().findByCloudId(it)?.id }
                    ?: 0
                val localSaleId = remote.saleCloudId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { db.saleDao().findByCloudId(it)?.id?.toString() }
                    ?: remote.billNumber.takeIf { it > 0 }?.toString()
                    ?: remote.saleId
                dao.insert(remote.toEntity(customerId, localSaleId))
            }
        }
    }

    private suspend fun syncSettings(uid: String) {
        val dao = db.storeSettingsDao()
        val local = dao.getOnce()
        val docRef = base(uid).collection("settings").document("settings")
        val remoteSnap = docRef.get().await()
        val remote = remoteSnap.toObject(SettingsRemote::class.java)

        if (local == null && remote == null) return

        if (local != null && (remote == null || local.updatedAt > (remote.updatedAt))) {
            docRef.set(local.toRemote(), SetOptions.merge()).await()
        } else if (local != null && remote != null && remote.updatedAt > local.updatedAt) {
            dao.save(remote.toEntity(local.id))
        } else if (local == null && remote != null) {
            dao.save(remote.toEntity(1))
        }
    }
}

data class CustomerRemote(
    val cloudId: String = "",
    val name: String = "",
    val phone: String = "",
    val creditBalance: Double = 0.0,
    val updatedAt: Long = 0
)

data class ItemRemote(
    val cloudId: String = "",
    val name: String = "",
    val price: Int = 0,
    val category: String? = null,
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)

data class CategoryRemote(
    val cloudId: String = "",
    val name: String = "",
    val isActive: Boolean = true,
    val updatedAt: Long = 0
)

data class SaleRemote(
    val cloudId: String = "",
    val billNumber: Int = 0,
    val timestamp: Long = 0,
    val customerCloudId: String? = null,
    val customerName: String? = null,
    val saleType: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val updatedAt: Long = 0
)

data class SaleItemRemote(
    val cloudId: String = "",
    val billNumber: Int = 0,
    val saleCloudId: String = "",
    val itemCloudId: String = "",
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Int = 0,
    val totalPrice: Double = 0.0,
    val timestamp: Long = 0,
    val updatedAt: Long = 0
)

data class CreditLedgerRemote(
    val cloudId: String = "",
    val customerCloudId: String? = null,
    val customerName: String = "",
    val saleId: String = "",
    val billNumber: Int = 0,
    val saleCloudId: String? = null,
    val timestamp: Long = 0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val updatedAt: Long = 0
)

data class SettingsRemote(
    val storeName: String = "",
    val phone: String = "",
    val primaryPrinterEnabled: Boolean = true,
    val printerType: String = "BLUETOOTH",
    val autoPrintSale: Boolean = true,
    val charactersPerLine: Int = 32,
    val column2Chars: Int = 4,
    val column3Chars: Int = 6,
    val column4Chars: Int = 6,
    val printTokenNumber: Boolean = false,
    val businessNameSize: String = "MEDIUM",
    val totalAmountSize: String = "SMALL",
    val printCreatedInfo: Boolean = true,
    val printFooter: String = "Thank You! Visit Again!",
    val printerSpacingFix: Boolean = false,
    val bottomPaddingLines: Int = 1,
    val printItemMultiLine: Boolean = true,
    val itemOrder: String = "INSERTED",
    val syncEnabled: Boolean = true,
    val syncHour: Int = 22,
    val syncMinute: Int = 0,
    val updatedAt: Long = 0
)

private fun CustomerEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "name" to name,
    "phone" to phone,
    "creditBalance" to creditBalance,
    "updatedAt" to updatedAt
)

private fun CustomerRemote.toEntity() = CustomerEntity(
    cloudId = cloudId,
    name = name,
    phone = phone,
    creditBalance = creditBalance,
    updatedAt = updatedAt
)

private fun ItemEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "name" to name,
    "price" to price,
    "category" to category,
    "isActive" to isActive,
    "updatedAt" to updatedAt
)

private fun ItemRemote.toEntity() = ItemEntity(
    cloudId = cloudId,
    name = name,
    price = price,
    category = category,
    isActive = isActive,
    updatedAt = updatedAt
)

private fun CategoryEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "name" to name,
    "isActive" to isActive,
    "updatedAt" to updatedAt
)

private fun CategoryRemote.toEntity() = CategoryEntity(
    cloudId = cloudId,
    name = name,
    isActive = isActive,
    updatedAt = updatedAt
)

private fun SaleEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "billNumber" to id,
    "timestamp" to timestamp,
    "customerCloudId" to customerCloudId,
    "customerName" to customerName,
    "saleType" to saleType,
    "totalAmount" to totalAmount,
    "paidAmount" to paidAmount,
    "dueAmount" to dueAmount,
    "updatedAt" to updatedAt
)

private fun SaleRemote.toEntity(customerId: Int?) = SaleEntity(
    cloudId = cloudId,
    id = billNumber,
    timestamp = timestamp,
    customerId = customerId,
    customerCloudId = customerCloudId,
    customerName = customerName,
    saleType = saleType,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    dueAmount = dueAmount,
    updatedAt = updatedAt
)

private fun SaleItemEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "billNumber" to saleId,
    "saleCloudId" to saleCloudId,
    "itemCloudId" to itemCloudId,
    "itemName" to itemName,
    "quantity" to quantity,
    "unitPrice" to unitPrice,
    "totalPrice" to totalPrice,
    "timestamp" to timestamp,
    "updatedAt" to updatedAt
)

private fun SaleItemRemote.toEntity(saleId: Int, itemId: Int) = SaleItemEntity(
    cloudId = cloudId,
    saleId = saleId,
    saleCloudId = saleCloudId,
    itemId = itemId,
    itemCloudId = itemCloudId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
    timestamp = timestamp,
    updatedAt = updatedAt
)

private fun CreditLedgerEntity.toRemote(cloudId: String) = mapOf(
    "cloudId" to cloudId,
    "customerCloudId" to customerCloudId,
    "customerName" to customerName,
    "saleId" to saleId,
    "billNumber" to (saleId.toIntOrNull() ?: 0),
    "saleCloudId" to saleCloudId,
    "timestamp" to timestamp,
    "totalAmount" to totalAmount,
    "paidAmount" to paidAmount,
    "dueAmount" to dueAmount,
    "updatedAt" to updatedAt
)

private fun CreditLedgerRemote.toEntity(customerId: Int, saleIdOverride: String) = CreditLedgerEntity(
    cloudId = cloudId,
    customerId = customerId,
    customerCloudId = customerCloudId,
    customerName = customerName,
    saleId = saleIdOverride,
    saleCloudId = saleCloudId,
    timestamp = timestamp,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    dueAmount = dueAmount,
    updatedAt = updatedAt
)

private fun StoreSettingsEntity.toRemote() = mapOf(
    "storeName" to storeName,
    "phone" to phone,
    "primaryPrinterEnabled" to primaryPrinterEnabled,
    "printerType" to printerType,
    "autoPrintSale" to autoPrintSale,
    "charactersPerLine" to charactersPerLine,
    "column2Chars" to column2Chars,
    "column3Chars" to column3Chars,
    "column4Chars" to column4Chars,
    "printTokenNumber" to printTokenNumber,
    "businessNameSize" to businessNameSize,
    "totalAmountSize" to totalAmountSize,
    "printCreatedInfo" to printCreatedInfo,
    "printFooter" to printFooter,
    "printerSpacingFix" to printerSpacingFix,
    "bottomPaddingLines" to bottomPaddingLines,
    "printItemMultiLine" to printItemMultiLine,
    "itemOrder" to itemOrder,
    "syncEnabled" to syncEnabled,
    "syncHour" to syncHour,
    "syncMinute" to syncMinute,
    "updatedAt" to updatedAt
)

private fun SettingsRemote.toEntity(id: Int) = StoreSettingsEntity(
    id = id,
    storeName = storeName,
    phone = phone,
    primaryPrinterEnabled = primaryPrinterEnabled,
    printerType = printerType,
    autoPrintSale = autoPrintSale,
    charactersPerLine = charactersPerLine,
    column2Chars = column2Chars,
    column3Chars = column3Chars,
    column4Chars = column4Chars,
    printTokenNumber = printTokenNumber,
    businessNameSize = businessNameSize,
    totalAmountSize = totalAmountSize,
    printCreatedInfo = printCreatedInfo,
    printFooter = printFooter,
    printerSpacingFix = printerSpacingFix,
    bottomPaddingLines = bottomPaddingLines,
    printItemMultiLine = printItemMultiLine,
    itemOrder = itemOrder,
    syncEnabled = syncEnabled,
    syncHour = syncHour,
    syncMinute = syncMinute,
    updatedAt = updatedAt
)

private fun normalize(value: String) = value.trim().lowercase()

private fun resolveCustomerCloudId(
    local: CustomerEntity,
    remotes: Collection<CustomerRemote>
): String? {
    if (local.cloudId.isNotBlank()) return local.cloudId
    val targetName = normalize(local.name)
    val targetPhone = normalize(local.phone)
    return remotes.firstOrNull {
        normalize(it.name) == targetName && normalize(it.phone) == targetPhone
    }?.cloudId
}

private fun resolveItemCloudId(
    local: ItemEntity,
    remotes: Collection<ItemRemote>
): String? {
    if (local.cloudId.isNotBlank()) return local.cloudId
    val targetName = normalize(local.name)
    val targetCategory = local.category?.let { normalize(it) } ?: ""
    return remotes.firstOrNull {
        normalize(it.name) == targetName &&
            it.price == local.price &&
            (it.category?.let { c -> normalize(c) } ?: "") == targetCategory
    }?.cloudId
}

private fun resolveCategoryCloudId(
    local: CategoryEntity,
    remotes: Collection<CategoryRemote>
): String? {
    if (local.cloudId.isNotBlank()) return local.cloudId
    val targetName = normalize(local.name)
    return remotes.firstOrNull { normalize(it.name) == targetName }?.cloudId
}
