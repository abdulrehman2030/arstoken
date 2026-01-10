package com.ar.arstoken.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.ar.arstoken.model.CartItem
import com.ar.arstoken.model.Item
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ar.arstoken.data.SaleRepository
import com.ar.arstoken.model.Customer
import com.ar.arstoken.model.PaymentMode
import androidx.lifecycle.viewModelScope
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.data.repository.CustomerRepository
import com.ar.arstoken.data.repository.ItemRepository
import kotlinx.coroutines.launch
import com.ar.arstoken.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class BillingViewModel(
    private val saleRepository: SaleRepository,
    private val itemRepository: ItemRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    val customers = customerRepository.getCustomers()
        .map { entities ->
            entities.map {
                Customer(
                    id = it.id,
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
                    name = it.name,
                    price = it.price
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    var selectedCustomer by mutableStateOf<Customer?>(null)
        private set

    var paymentMode by mutableStateOf(PaymentMode.CASH)
        private set

    var partialPaidAmount by mutableStateOf(0.0)
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
                    qty = 1.0
                )
            )
        }
    }

    // --------------------
    // Add item with custom qty / price (long press)
    // --------------------
    fun addItemWithCustomPrice(
        item: Item,
        quantity: Double,
        totalPrice: Double
    ) {
        if (quantity <= 0 || totalPrice <= 0) return

        val unitPrice = totalPrice / quantity

        if (unitPrice.isNaN() || unitPrice.isInfinite()) return

        _cart.add(
            CartItem(
                item = item.copy(price = unitPrice),
                qty = quantity
            )
        )
    }


    // --------------------
    // Total calculation
    // --------------------
    fun getTotal(): Double {
        return _cart.sumOf { it.item.price * it.qty }
    }

    fun selectCustomer(customer: Customer?) {
        selectedCustomer = customer

        if (customer == null) {
            paymentMode = PaymentMode.CASH
            partialPaidAmount = 0.0
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

    fun getCartQuantity(itemId: Int): Double? {
        return _cart.find { it.item.id == itemId }?.qty
    }

    fun removeItemFromCart(itemId: Int) {
        _cart.removeAll { it.item.id == itemId }
    }

    fun updatePaymentMode(mode: PaymentMode) {
        paymentMode = mode
        if (mode != PaymentMode.PARTIAL) {
            partialPaidAmount = 0.0
        }
    }

    fun setPartialPaidAmount(input: String) {
        partialPaidAmount = input.toDoubleOrNull() ?: 0.0
    }

    // --------------------
    // Save sale (placeholder)
    // --------------------
    fun proceedSale() {
        if (cart.isEmpty()) return

        val total = getTotal()
        if (total <= 0 || total.isNaN()) return

        // Enforce customer for CREDIT / PARTIAL
        if (
            (paymentMode == PaymentMode.CREDIT || paymentMode == PaymentMode.PARTIAL)
            && selectedCustomer == null
        ) return

        val paidAmount: Double = when (paymentMode) {
            PaymentMode.CASH -> total

            PaymentMode.CREDIT -> 0.0

            PaymentMode.PARTIAL -> {
                if (partialPaidAmount <= 0.0) return
                if (partialPaidAmount >= total) return
                partialPaidAmount
            }
        }

        val customerId = selectedCustomer?.id ?: 0
        val customerName = selectedCustomer?.name ?: "Retail"

        val sale = SaleEntity(
            timestamp = System.currentTimeMillis(),
            customerId = customerId,
            customerName = customerName,
            saleType = paymentMode.name,
            totalAmount = total,
            paidAmount = paidAmount,
            dueAmount = total - paidAmount
        )

        viewModelScope.launch {
            val saleId = saleRepository.saveSale(sale)

            val saleItems = cart.map { cartItem ->
                SaleItemEntity(
                    saleId = saleId.toInt(),
                    itemId = cartItem.item.id,
                    itemName = cartItem.item.name,
                    quantity = cartItem.qty,
                    unitPrice = cartItem.item.price,
                    totalPrice = cartItem.qty * cartItem.item.price,
                    timestamp = System.currentTimeMillis()
                )
            }

            saleRepository.saveSaleItems(saleItems)
            clearCart()
        }
    }



    private fun Sale.toEntity(): SaleEntity {
        return SaleEntity(
            customerId = customerId,
            customerName = customerName,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            dueAmount = totalAmount - paidAmount,
            saleType = saleType.toString(),
            timestamp = timestamp
        )

    }


    private fun resetExtras() {
        selectedCustomer = null
        paymentMode = PaymentMode.CASH
        partialPaidAmount = 0.0
    }


    // --------------------
    // CLEAR CART (THIS WAS MISSING)
    // --------------------
    fun clearCart() {
        _cart.clear()
    }
}
