//package com.ar.arstoken.ui.billing
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Remove
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.text.font.FontWeight
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.ar.arstoken.viewmodel.BillingViewModel
//import com.ar.arstoken.model.CartItem
//import com.ar.arstoken.navigation.NavRoutes
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CartScreen(
//    viewModel: BillingViewModel,
//    onProceedToPayment: () -> Unit
//) {
//    val cart = viewModel.cart
//    val total = viewModel.getTotal()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Cart") }
//            )
//        },
//        bottomBar = {
//            if (cart.isNotEmpty()) {
//                CartTotalBar(
//                    total = total,
//                    onProceedClick = onProceedToPayment
//                )
//            }
//        }
//    ) { paddingValues ->
//
//        LazyColumn(
//            modifier = Modifier
//                .padding(paddingValues)
//                .padding(8.dp)
//        ) {
//            items(cart) { cartItem ->
//                CartItemRow(
//                    cartItem = cartItem,
//                    onIncrease = { viewModel.increaseQty(cartItem) },
//                    onDecrease = { viewModel.decreaseQty(cartItem) },
//                    onRemove = { viewModel.removeItem(cartItem) }
//                )
//            }
//        }
//    }
//}
//
//
