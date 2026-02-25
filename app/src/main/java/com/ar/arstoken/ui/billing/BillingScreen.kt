package com.ar.arstoken.ui.billing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.arstoken.model.Item
import com.ar.arstoken.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel,
    onOpenReports: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenItems: () -> Unit,
    onOpenSettings: () -> Unit,
    showSavedMessage: Boolean,
    onSnackbarShown: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val total = viewModel.getTotal()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var editQuantity by remember { mutableStateOf(1.0) }
    var editTotalPrice by remember { mutableStateOf(0.0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val moreSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // 🔔 Show snackbar when coming back from Settings
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            snackbarHostState.showSnackbar("Saved successfully")
            onSnackbarShown()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {

            ModalDrawerSheet {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close menu"
                        )
                    }

                    Text(
                        "Admin",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Customers") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenCustomers()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Items") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenItems()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Reports") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenReports()
                    }
                )

                // ⭐ THIS IS YOUR SETTINGS ENTRY
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()   // 👈 HERE
                    }
                )
            }
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("ARS Token")
                            Text(
                                "Tap to add • Long press to edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                BottomActionBar(
                    total = total,
                    onProceed = {
                        if (viewModel.cart.isNotEmpty()) {
                            viewModel.proceedSale()
                            scope.launch {
                                snackbarHostState.showSnackbar("Bill saved")
                            }
                        }
                    },
                    onMore = {
                        scope.launch {
                            if (drawerState.isOpen) {
                                drawerState.close()
                            }
                            showMoreSheet = true
                        }
                    }

                )
            }
        ) { padding ->

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items) { item ->
                    ItemTile(
                        name = item.name,
                        price = item.price,
                        quantityInCart = viewModel.getCartQuantity(item.id),
                        onClick = {
                            viewModel.addItemToCart(item)
                        },
                        onLongPress = {
                            val cartItem = viewModel.getCartItem(item.id)
                            if (cartItem != null) {
                                selectedItem = cartItem.item
                                editQuantity = cartItem.qty
                                editTotalPrice = cartItem.qty * cartItem.item.price
                            } else {
                                selectedItem = item
                                editQuantity = 1.0
                                editTotalPrice = item.price
                            }

                            showEditSheet = true
                        }

                    )

                }
            }
        }
        if (showEditSheet && selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false }
            ) {
                EditItemSheet(
                    item = selectedItem!!,
                    initialQty = editQuantity,
                    initialTotalPrice = editTotalPrice,
                    onConfirm = { qty, totalPrice ->
                        viewModel.addItemWithCustomPrice(
                            item = selectedItem!!,
                            quantity = qty,
                            totalPrice = totalPrice
                        )
                        showEditSheet = false
                    },
                    onDiscard = {
                        viewModel.removeItemFromCart(selectedItem!!.id)
                        showEditSheet = false
                    },
                    onDismiss = {
                        showEditSheet = false
                    }
                )


            }
        }
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                sheetState = moreSheetState
            ) {
                val customers by viewModel.customers.collectAsState()

                MoreOptionsSheet(
                    customers = customers,
                    selectedCustomer = viewModel.selectedCustomer,
                    total = total,
                    paymentMode = viewModel.paymentMode,
                    partialPaidAmount = viewModel.partialPaidAmount,
                    onCustomerSelected = viewModel::selectCustomer,
                    onPaymentModeChange = viewModel::updatePaymentMode,
                    onPartialAmountChange = viewModel::setPartialPaidAmount,
                    onSummary = { showMoreSheet = false }
                )

            }
        }

    }

}
