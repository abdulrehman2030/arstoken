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
import com.ar.arstoken.ui.auth.PhoneLoginScreen
import com.ar.arstoken.ui.billing.BillingScreen
import com.ar.arstoken.ui.categories.NewCategoryScreen
import com.ar.arstoken.ui.customers.CustomerLedgerScreen
import com.ar.arstoken.ui.customers.CustomersScreen
import com.ar.arstoken.ui.items.ItemsScreen
import com.ar.arstoken.ui.reports.ItemSalesReportScreen
import com.ar.arstoken.ui.settings.BusinessProfileScreen
import com.ar.arstoken.ui.settings.PrintSettingsScreen
import com.ar.arstoken.ui.settings.SettingsLandingScreen
import com.ar.arstoken.ui.settings.SettingsScreen
import com.ar.arstoken.ui.theme.ARSTokenTheme
import com.ar.arstoken.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

enum class AdminScreen {
    BILLING,
    REPORTS,
    CUSTOMERS,
    ITEMS,
    CATEGORY_CREATE,
    CUSTOMER_LEDGER,
    SETTINGS_LANDING,
    SETTINGS,
    PRINT_SETTINGS,
    BUSINESS_PROFILE
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
                val auth = remember { FirebaseAuth.getInstance() }
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        isLoggedIn = firebaseAuth.currentUser != null
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

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
                val profileRepo = remember {
                    BusinessProfileRepository(
                        dao = db.businessProfileDao(),
                        firestore = FirebaseFirestore.getInstance(),
                        storage = FirebaseStorage.getInstance()
                    )
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
                val uid = auth.currentUser?.uid
                val phoneNumber = auth.currentUser?.phoneNumber
                val profileViewModel = remember(uid) {
                    if (uid == null) null else BusinessProfileViewModel(uid, profileRepo)
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

                if (!isLoggedIn) {
                    PhoneLoginScreen(
                        onSignedIn = { isLoggedIn = true }
                    )
                    return@ARSTokenTheme
                }
                profileViewModel?.startSync { }
                val profileState = profileViewModel?.profile?.collectAsState()
                val businessName = profileState?.value?.businessName?.takeIf { it.isNotBlank() }
                val logoUrl = profileState?.value?.logoUrl
                val hasProfile = profileState?.value != null

                LaunchedEffect(hasProfile, isLoggedIn) {
                    if (isLoggedIn && hasProfile == false && currentScreen != AdminScreen.BUSINESS_PROFILE) {
                        currentScreen = AdminScreen.BUSINESS_PROFILE
                    }
                }

                when (currentScreen) {

                    AdminScreen.BILLING -> {
                        BillingScreen(
                            viewModel = billingViewModel,
                            businessName = businessName ?: "ARS Token",
                            logoUrl = logoUrl,
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
                            onOpenBusinessProfile = {
                                currentScreen = AdminScreen.BUSINESS_PROFILE
                            },
                            onOpenPrintSettings = {
                                currentScreen = AdminScreen.PRINT_SETTINGS
                            },
                            onSignOut = {
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AdminScreen.BILLING
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
                    AdminScreen.BUSINESS_PROFILE -> {
                        val vm = profileViewModel
                        if (vm != null) {
                            BusinessProfileScreen(
                                viewModel = vm,
                                phoneNumber = phoneNumber,
                                onBack = {
                                    currentScreen = AdminScreen.SETTINGS_LANDING
                                },
                                onSaved = {
                                    currentScreen = AdminScreen.BILLING
                                }
                            )
                        } else {
                            currentScreen = AdminScreen.SETTINGS_LANDING
                        }
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
