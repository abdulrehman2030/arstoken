package com.ar.arstoken.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ar.arstoken.data.SaleRepository
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.db.CreditLedgerEntity
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.data.repository.CustomerRepository
import com.ar.arstoken.data.repository.ItemRepository
import com.ar.arstoken.data.repository.SettingsRepository
import com.ar.arstoken.model.CartItem
import com.ar.arstoken.model.Customer
import com.ar.arstoken.model.Item
import com.ar.arstoken.model.PaymentMode
import com.ar.arstoken.util.formatReceipt
import com.ar.arstoken.util.newCloudId
import com.ar.arstoken.util.nowMs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class BillingViewModel(
    private val saleRepository: SaleRepository,
    private val itemRepository: ItemRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val customers = customerRepository.getCustomers()
        .map { entities ->
            entities.map {
                Customer(
                    id = it.id,
                    cloudId = it.cloudId,
                    name = it.name,
                    phone = it.phone,
                    creditBalance = it.creditBalance
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )


    val items = itemRepository.getItems()
        .map { entityList ->
            entityList.map {
                Item(
                    id = it.id,
                    cloudId = it.cloudId,
                    name = it.name,
                    price = it.price,
                    category = it.category
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val storeSettings = settingsRepository.observe()
        .map {
            it ?: StoreSettingsEntity(
                storeName = "My Store",
                phone = ""
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StoreSettingsEntity(storeName = "My Store", phone = "")
        )

    var selectedCustomer by mutableStateOf<Customer?>(null)
        private set

    var paymentMode by mutableStateOf(PaymentMode.CASH)
        private set

    var partialPaidAmount by mutableStateOf(0)
        private set


    // --------------------
    // Cart state
    // --------------------
    private val _cart = mutableStateListOf<CartItem>()
    val cart: List<CartItem> = _cart

    // --------------------
    // Add item (tap)
    // --------------------
    fun addItemToCart(item: Item) {
        val index = _cart.indexOfFirst { it.item.id == item.id }

        if (index >= 0) {
            val existing = _cart[index]
            _cart[index] = existing.copy(
                qty = existing.qty + 1
            )
        } else {
            _cart.add(
                CartItem(
                    item = item,
                    qty = 1
                )
            )
        }
    }

    // --------------------
    // Add item with custom qty / price (long press)
    // --------------------
    fun addItemWithCustomPrice(
        item: Item,
        quantity: Int,
        totalPrice: Int
    ) {
        if (quantity <= 0 || totalPrice <= 0) return

        val unitPrice = totalPrice / quantity

        val updatedItem = CartItem(
            item = item.copy(price = unitPrice),
            qty = quantity
        )

        // Keep one cart row per item id to avoid duplicated totals/badges.
        _cart.removeAll { it.item.id == item.id }
        _cart.add(updatedItem)
    }


    // --------------------
    // Total calculation
    // --------------------
    fun getTotal(): Int {
        return _cart.sumOf { it.item.price * it.qty }
    }

    fun selectCustomer(customer: Customer?) {
        selectedCustomer = customer

        if (customer == null) {
            paymentMode = PaymentMode.CASH
            partialPaidAmount = 0
        }
    }

//    fun setPaymentMode(mode: PaymentMode) {
//        paymentMode = mode
//        if (mode != PaymentMode.PARTIAL) {
//            partialPaidAmount = ""
//        }
//    }

    fun getCartItem(itemId: Int): CartItem? {
        return _cart.find { it.item.id == itemId }
    }

    fun getCartQuantity(itemId: Int): Int? {
        return _cart.find { it.item.id == itemId }?.qty
    }

    fun removeItemFromCart(itemId: Int) {
        _cart.removeAll { it.item.id == itemId }
    }

    fun updatePaymentMode(mode: PaymentMode) {
        paymentMode = mode
        if (mode != PaymentMode.PARTIAL) {
            partialPaidAmount = 0
        }
    }

    fun setPartialPaidAmount(input: String) {
        partialPaidAmount = input.toIntOrNull() ?: 0
    }

    // --------------------
    // Save sale (placeholder)
    // --------------------
    fun proceedSale(
        businessNameOverride: String? = null,
        onReceiptReady: (String) -> Unit = {},
        onSaleSaved: () -> Unit = {}
    ) {
        if (cart.isEmpty()) return

        val total = getTotal()
        if (total <= 0) return

        val customerId = selectedCustomer?.id ?: 0
        val customerName = selectedCustomer?.name ?: "Retail"

        val paidAmount = when (paymentMode) {
            PaymentMode.CASH -> total
            PaymentMode.CREDIT -> 0
            PaymentMode.PARTIAL -> partialPaidAmount
        }

        if (paymentMode == PaymentMode.PARTIAL &&
            (paidAmount <= 0 || paidAmount >= total)
        ) return

        val saleCloudId = newCloudId()
        val now = nowMs()
        val sale = SaleEntity(
            cloudId = saleCloudId,
            timestamp = System.currentTimeMillis(),
            customerId = customerId,
            customerCloudId = selectedCustomer?.cloudId,
            customerName = customerName,
            saleType = paymentMode.name,
            totalAmount = total,
            paidAmount = paidAmount,
            dueAmount = total - paidAmount,
            updatedAt = now
        )

        viewModelScope.launch {

            // 1️⃣ SAVE SALE
            val saleId = saleRepository.saveSale(sale)

            // 2️⃣ MAP CART → SALE ITEMS
            val saleItems = cart.map { cartItem ->
                SaleItemEntity(
                    cloudId = newCloudId(),
                    saleId = saleId.toInt(),
                    saleCloudId = saleCloudId,
                    itemId = cartItem.item.id,
                    itemCloudId = cartItem.item.cloudId,
                    itemName = cartItem.item.name,
                    quantity = cartItem.qty,
                    unitPrice = cartItem.item.price,
                    totalPrice = cartItem.qty * cartItem.item.price,
                    timestamp = System.currentTimeMillis(),
                    updatedAt = now
                )
            }

            // 3️⃣ SAVE ITEMS
            saleRepository.saveSaleItems(saleItems)

            // 3️⃣b SAVE LEDGER ENTRY (customer credit tracking)
            if (customerId > 0 && sale.dueAmount > 0) {
                saleRepository.saveCreditLedgerEntry(
                    CreditLedgerEntity(
                        cloudId = newCloudId(),
                        customerId = customerId,
                        customerCloudId = selectedCustomer?.cloudId,
                        customerName = customerName,
                        saleId = saleId.toString(),
                        saleCloudId = saleCloudId,
                        timestamp = sale.timestamp,
                        totalAmount = sale.totalAmount,
                        paidAmount = sale.paidAmount,
                        dueAmount = sale.dueAmount,
                        updatedAt = now
                    )
                )
            }

            // 4️⃣ PRINT
            val settings = storeSettings.value

            val printableItems = when (settings.itemOrder) {
                "ALPHABETICAL" -> saleItems.sortedBy { it.itemName.lowercase() }
                else -> saleItems
            }
            val receipt = formatReceipt(
                settings = settings,
                businessNameOverride = businessNameOverride,
                sale = sale.copy(id = saleId.toInt()),
                items = printableItems
            )

            if (settings.primaryPrinterEnabled && settings.autoPrintSale) {
                onReceiptReady(receipt)
            }

            clearCart()
            // Reset bill options for the next bill.
            selectedCustomer = null
            paymentMode = PaymentMode.CASH
            partialPaidAmount = 0
            onSaleSaved()
        }
    }


    // --------------------
    // CLEAR CART (THIS WAS MISSING)
    // --------------------
    fun clearCart() {
        _cart.clear()
    }
}
