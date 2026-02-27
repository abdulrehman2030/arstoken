package com.ar.arstoken

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.data.repository.*
import com.ar.arstoken.model.Customer
import com.ar.arstoken.ui.auth.PhoneLoginScreen
import com.ar.arstoken.ui.billing.BillingScreen
import com.ar.arstoken.ui.categories.NewCategoryScreen
import com.ar.arstoken.ui.customers.CustomerLedgerScreen
import com.ar.arstoken.ui.customers.CustomersScreen
import com.ar.arstoken.ui.items.ItemsScreen
import com.ar.arstoken.ui.reports.ItemSalesReportScreen
import com.ar.arstoken.ui.reports.BillDetailScreen
import com.ar.arstoken.ui.settings.BusinessProfileScreen
import com.ar.arstoken.ui.settings.PrintSettingsScreen
import com.ar.arstoken.ui.settings.SettingsLandingScreen
import com.ar.arstoken.ui.theme.ARSTokenTheme
import com.ar.arstoken.viewmodel.*
import com.ar.arstoken.viewmodel.BillDetailViewModel
import com.ar.arstoken.data.sync.CloudSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AdminScreen {
    BILLING,
    REPORTS,
    CUSTOMERS,
    ITEMS,
    CATEGORY_CREATE,
    CUSTOMER_LEDGER,
    SETTINGS_LANDING,
    PRINT_SETTINGS,
    BUSINESS_PROFILE,
    BILL_DETAIL
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
                val settingsState by settingsViewModel.settings.collectAsState()

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
                var selectedCustomerCloudId by rememberSaveable { mutableStateOf<String?>(null) }
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
                val syncManager = remember { CloudSyncManager(db, FirebaseFirestore.getInstance()) }
                val scope = rememberCoroutineScope()

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
                var selectedSaleId by rememberSaveable { mutableStateOf<Int?>(null) }

                if (!isLoggedIn) {
                    PhoneLoginScreen(
                        onSignedIn = {
                            isLoggedIn = true
                            val currentUid = auth.currentUser?.uid
                            if (currentUid != null) {
                                scope.launch {
                                    try {
                                        syncManager.syncAll(currentUid)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    )
                    return@ARSTokenTheme
                }
                profileViewModel?.startSync { }

                val syncEnabled = settingsState?.syncEnabled ?: true
                val syncHour = settingsState?.syncHour ?: 22
                val syncMinute = settingsState?.syncMinute ?: 0
                LaunchedEffect(isLoggedIn, syncEnabled, syncHour, syncMinute, uid) {
                    if (!isLoggedIn || !syncEnabled || uid == null) return@LaunchedEffect
                    while (isActive) {
                        val now = Calendar.getInstance()
                        val next = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, syncHour)
                            set(Calendar.MINUTE, syncMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (before(now)) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        val delayMs = next.timeInMillis - now.timeInMillis
                        delay(delayMs)
                        if (isLoggedIn) {
                            try {
                                syncManager.syncAll(uid)
                            } catch (_: Exception) {
                            }
                        }
                        delay(1_000L)
                    }
                }
                val profileState = profileViewModel?.profile?.collectAsState()
                val businessName = profileState?.value?.businessName?.takeIf { it.isNotBlank() }
                val businessPhone = profileState?.value?.phone?.takeIf { it.isNotBlank() }
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
                            businessPhone = businessPhone,
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
                            ItemSalesViewModel(reportRepo, db, settingsRepo)
                        }

                        ItemSalesReportScreen(
                            viewModel = vm,
                            onBack = {
                                currentScreen = AdminScreen.BILLING   // 👈 THIS WAS MISSING
                            },
                            onSaleSelected = { saleId ->
                                selectedSaleId = saleId
                                currentScreen = AdminScreen.BILL_DETAIL
                            },
                            settings = settingsState ?: StoreSettingsEntity(storeName = "My Store", phone = ""),
                            businessName = businessName,
                            businessPhone = businessPhone
                        )
                    }
                    AdminScreen.BILL_DETAIL -> {
                        val saleId = selectedSaleId
                        if (saleId != null) {
                            val detailVm = remember(saleId) {
                                BillDetailViewModel(db, saleId)
                            }
                            BillDetailScreen(
                                viewModel = detailVm,
                                settings = settingsState ?: StoreSettingsEntity(storeName = "My Store", phone = ""),
                                businessName = businessName,
                                businessPhone = businessPhone,
                                onBack = {
                                    currentScreen = AdminScreen.REPORTS
                                }
                            )
                        } else {
                            currentScreen = AdminScreen.REPORTS
                        }
                    }

                    AdminScreen.CUSTOMERS -> {
                        CustomersScreen(
                            viewModel = customerViewModel,
                            onCustomerSelected = {
                                selectedCustomerId = it.id
                                selectedCustomerCloudId = it.cloudId
                                selectedCustomerName = it.name
                                selectedCustomerPhone = it.phone
                                currentScreen = AdminScreen.CUSTOMER_LEDGER
                            },
                            onBack = { currentScreen = AdminScreen.BILLING }
                        )
                    }

                    AdminScreen.CUSTOMER_LEDGER -> {
                        val customerId = selectedCustomerId
                        val customerCloudId = selectedCustomerCloudId
                        val customerName = selectedCustomerName
                        val customerPhone = selectedCustomerPhone
                        if (customerId != null && customerName != null) {
                            val ledgerVm = remember(customerId, customerName, customerCloudId) {
                                CustomerLedgerViewModel(
                                    customerId = customerId,
                                    customerName = customerName,
                                    customerCloudId = customerCloudId,
                                    saleRepository = saleRepo
                                )
                            }

                            CustomerLedgerScreen(
                                customerName = customerName,
                                customerPhone = customerPhone.orEmpty(),
                                businessName = businessName,
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
                            onOpenBusinessProfile = {
                                currentScreen = AdminScreen.BUSINESS_PROFILE
                            },
                            onOpenPrintSettings = {
                                currentScreen = AdminScreen.PRINT_SETTINGS
                            },
                            onSignOut = {
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AdminScreen.BILLING
                            },
                            settings = settingsState,
                            onSyncNow = {
                                if (uid != null) {
                                    scope.launch {
                                        try {
                                            syncManager.syncAll(uid)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            },
                            onSaveSyncTime = { hour, minute ->
                                val current = settingsState ?: StoreSettingsEntity(storeName = "My Store", phone = "")
                                settingsViewModel.save(
                                    current.copy(
                                        syncHour = hour,
                                        syncMinute = minute
                                    )
                                )
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
