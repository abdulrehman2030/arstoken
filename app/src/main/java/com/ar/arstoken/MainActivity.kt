package com.ar.arstoken

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.repository.*
import com.ar.arstoken.model.Customer
import com.ar.arstoken.ui.billing.BillingScreen
import com.ar.arstoken.ui.categories.NewCategoryScreen
import com.ar.arstoken.ui.customers.CustomerLedgerScreen
import com.ar.arstoken.ui.customers.CustomersScreen
import com.ar.arstoken.ui.items.ItemsScreen
import com.ar.arstoken.ui.reports.ItemSalesReportScreen
import com.ar.arstoken.ui.settings.PrintSettingsScreen
import com.ar.arstoken.ui.settings.SettingsLandingScreen
import com.ar.arstoken.ui.settings.SettingsScreen
import com.ar.arstoken.ui.theme.ARSTokenTheme
import com.ar.arstoken.viewmodel.*

enum class AdminScreen {
    BILLING,
    REPORTS,
    CUSTOMERS,
    ITEMS,
    CATEGORY_CREATE,
    CUSTOMER_LEDGER,
    SETTINGS_LANDING,
    SETTINGS,
    PRINT_SETTINGS
}


class MainActivity : ComponentActivity() {

    @SuppressLint("ViewModelConstructorInComposable")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ARSTokenTheme {

                // ----------------------------
                // App-level navigation state
                // ----------------------------
                var currentScreen by rememberSaveable {
                    mutableStateOf(AdminScreen.BILLING)
                }
                var showSavedMessage by remember { mutableStateOf(false) }

                // ----------------------------
                // Database (single instance)
                // ----------------------------
                val db = remember {
                    AppDatabase.get(applicationContext)
                }
                val settingsRepo = remember {
                    RoomSettingsRepository(db.storeSettingsDao())
                }

                val settingsViewModel = remember {
                    SettingsViewModel(settingsRepo)
                }

                // ----------------------------
                // Repositories (single instance)
                // ----------------------------
                val saleRepo = remember { RoomSaleRepository(db) }
                val reportRepo: RoomReportRepository = RoomReportRepository(db)

                val customerRepo = remember { RoomCustomerRepository(db) }
                val itemRepo = remember { RoomItemRepository(db) }
                val itemViewModel = remember {
                    ItemViewModel(itemRepo)
                }
                val categoryViewModel = remember {
                    CategoryViewModel(itemRepo)
                }
                var selectedCustomerId by rememberSaveable { mutableStateOf<Int?>(null) }
                var selectedCustomerName by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedCustomerPhone by rememberSaveable { mutableStateOf<String?>(null) }
                val customerViewModel = remember {
                    CustomerViewModel(
                        customerRepository = customerRepo,
                        saleRepository = saleRepo
                    )
                }

                // ----------------------------
                // Screen routing
                // ----------------------------
                val billingViewModel = remember {
                    BillingViewModel(
                        saleRepository = saleRepo,
                        itemRepository = itemRepo,
                        customerRepository = customerRepo,
                        settingsRepository = settingsRepo
                    )
                }
                when (currentScreen) {

                    AdminScreen.BILLING -> {
                        BillingScreen(
                            viewModel = billingViewModel,
                            onOpenReports = {
                                currentScreen = AdminScreen.REPORTS
                            },
                            onOpenCustomers = {
                                currentScreen = AdminScreen.CUSTOMERS
                            },
                            onOpenItems = {
                                currentScreen = AdminScreen.ITEMS
                            },
                            onOpenSettings = {          // 👈 ADD
                                currentScreen = AdminScreen.SETTINGS_LANDING
                            },
                            showSavedMessage = showSavedMessage,
                            onSnackbarShown = { showSavedMessage = false }
                        )
                    }

                    AdminScreen.REPORTS -> {
                        val vm = remember(reportRepo) {
                            ItemSalesViewModel(reportRepo)
                        }

                        ItemSalesReportScreen(
                            viewModel = vm,
                            onBack = {
                                currentScreen = AdminScreen.BILLING   // 👈 THIS WAS MISSING
                            }
                        )
                    }

                    AdminScreen.CUSTOMERS -> {
                        CustomersScreen(
                            viewModel = customerViewModel,
                            onCustomerSelected = {
                                selectedCustomerId = it.id
                                selectedCustomerName = it.name
                                selectedCustomerPhone = it.phone
                                currentScreen = AdminScreen.CUSTOMER_LEDGER
                            },
                            onBack = { currentScreen = AdminScreen.BILLING }
                        )
                    }

                    AdminScreen.CUSTOMER_LEDGER -> {
                        val customerId = selectedCustomerId
                        val customerName = selectedCustomerName
                        val customerPhone = selectedCustomerPhone
                        if (customerId != null && customerName != null) {
                            val ledgerVm = remember(customerId, customerName) {
                                CustomerLedgerViewModel(
                                    customerId = customerId,
                                    customerName = customerName,
                                    saleRepository = saleRepo
                                )
                            }

                            CustomerLedgerScreen(
                                customerName = customerName,
                                customerPhone = customerPhone.orEmpty(),
                                viewModel = ledgerVm,
                                onBack = {
                                    currentScreen = AdminScreen.CUSTOMERS
                                }
                            )
                        }
                        else {
                            currentScreen = AdminScreen.CUSTOMERS
                        }
                    }

                    AdminScreen.ITEMS -> {
                        ItemsScreen(
                            viewModel = itemViewModel,
                            onAddCategory = {
                                currentScreen = AdminScreen.CATEGORY_CREATE
                            },
                            onBack = {
                                currentScreen = AdminScreen.BILLING
                            }
                        )
                    }
                    AdminScreen.SETTINGS_LANDING -> {
                        SettingsLandingScreen(
                            onBack = {
                                currentScreen = AdminScreen.BILLING
                            },
                            onOpenStoreSettings = {
                                currentScreen = AdminScreen.SETTINGS
                            },
                            onOpenPrintSettings = {
                                currentScreen = AdminScreen.PRINT_SETTINGS
                            }
                        )
                    }
                    AdminScreen.SETTINGS -> {

                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = {
                                currentScreen = AdminScreen.SETTINGS_LANDING
                            },
                            onSaved = {
                                showSavedMessage = true
                                currentScreen = AdminScreen.BILLING
                            }
                        )
                    }
                    AdminScreen.PRINT_SETTINGS -> {
                        PrintSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = {
                                currentScreen = AdminScreen.SETTINGS_LANDING
                            },
                            onSaved = {
                                showSavedMessage = true
                                currentScreen = AdminScreen.BILLING
                            }
                        )
                    }
                    AdminScreen.CATEGORY_CREATE -> {
                        NewCategoryScreen(
                            viewModel = categoryViewModel,
                            onBack = {
                                currentScreen = AdminScreen.ITEMS
                            },
                            onCategoryAdded = { categoryName ->
                                itemViewModel.updateDraftCategory(categoryName)
                                currentScreen = AdminScreen.ITEMS
                            }
                        )
                    }

                }
            }
        }
    }
}
