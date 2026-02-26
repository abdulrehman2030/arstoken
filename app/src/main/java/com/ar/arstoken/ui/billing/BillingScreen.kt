package com.ar.arstoken.ui.billing

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ar.arstoken.model.Item
import com.ar.arstoken.util.ThermalPrinterHelper
import com.ar.arstoken.viewmodel.BillingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



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
    val storeSettings by viewModel.storeSettings.collectAsState()
    val items by viewModel.items.collectAsState()
    val categories = remember(items) {
        items.mapNotNull { it.category?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val filteredItems = remember(items, selectedCategory) {
        if (selectedCategory == null) items
        else items.filter { it.category == selectedCategory }
    }
    val total = viewModel.getTotal()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var editQuantity by remember { mutableStateOf(1) }
    var editTotalPrice by remember { mutableStateOf(0) }
    var pendingReceipt by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val moreSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasBtConnectPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    suspend fun printReceipt(receipt: String) {
        val result = withContext(Dispatchers.IO) {
            ThermalPrinterHelper().printToPairedPrinter(
                text = receipt,
                options = ThermalPrinterHelper.PrintOptions(
                    bottomPaddingLines = storeSettings.bottomPaddingLines,
                    spacingFix = storeSettings.printerSpacingFix
                )
            )
        }
        if (result.success) {
            snackbarHostState.showSnackbar(result.message)
        } else {
            snackbarHostState.showSnackbar("Print failed: ${result.message}")
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth permission denied")
            }
            pendingReceipt = null
            return@rememberLauncherForActivityResult
        }

        val receipt = pendingReceipt ?: return@rememberLauncherForActivityResult
        scope.launch {
            printReceipt(receipt)
        }
        pendingReceipt = null
    }

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
                            Text(
                                storeSettings.storeName.ifBlank { "ARS Token" }
                            )
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
                            viewModel.proceedSale(
                                onReceiptReady = { receipt ->
                                    if (hasBtConnectPermission) {
                                        scope.launch {
                                            printReceipt(receipt)
                                        }
                                    } else {
                                        pendingReceipt = receipt
                                        bluetoothPermissionLauncher.launch(
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        )
                                    }
                                },
                                onSaleSaved = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Bill saved")
                                    }
                                }
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cart is empty")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (selectedCategory != null && categories.none { it == selectedCategory }) {
                    selectedCategory = null
                }

                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredItems) { item ->
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
                                    editQuantity = 1
                                    editTotalPrice = item.price
                                }

                                showEditSheet = true
                            },
                            onRemove = {
                                viewModel.removeItemFromCart(item.id)
                            }

                        )

                    }
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
