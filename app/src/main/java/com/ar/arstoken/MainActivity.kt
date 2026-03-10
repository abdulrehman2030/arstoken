package com.ar.arstoken

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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
import com.ar.arstoken.ui.settings.BackupSettingsScreen
import com.ar.arstoken.ui.settings.PrintSettingsScreen
import com.ar.arstoken.ui.settings.SettingsLandingScreen
import com.ar.arstoken.ui.theme.ARSTokenTheme
import com.ar.arstoken.viewmodel.*
import com.ar.arstoken.viewmodel.BillDetailViewModel
import com.ar.arstoken.data.sync.CloudSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AdminScreen {
    BILLING,
    REPORTS,
    CUSTOMERS,
    ITEMS,
    CATEGORY_CREATE,
    CUSTOMER_LEDGER,
    SETTINGS_LANDING,
    BACKUP_SETTINGS,
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
                var lastLoginRefreshUid by rememberSaveable { mutableStateOf<String?>(null) }
                var openDrawerOnBilling by rememberSaveable { mutableStateOf(false) }
                var drawerHighlightOnOpen by rememberSaveable {
                    mutableStateOf(AdminScreen.BILLING)
                }

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
                val firestore = remember { FirebaseFirestore.getInstance() }
                var accountStatus by remember(uid) { mutableStateOf<String?>(null) }
                var planStatus by remember(uid) { mutableStateOf<String?>(null) }
                var trialEndAtMs by remember(uid) { mutableStateOf<Long?>(null) }
                var subscriptionEndAtMs by remember(uid) { mutableStateOf<Long?>(null) }
                var approvalLoading by remember(uid) { mutableStateOf(uid != null) }
                var approvalRefreshNonce by remember(uid) { mutableStateOf(0) }
                val profileViewModel = remember(uid) {
                    if (uid == null) null else BusinessProfileViewModel(uid, profileRepo)
                }
                val syncManager = remember { CloudSyncManager(db, firestore) }
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
                    lastLoginRefreshUid = null
                    PhoneLoginScreen(
                        onSignedIn = {
                            isLoggedIn = true
                        }
                    )
                    return@ARSTokenTheme
                }

                DisposableEffect(uid, isLoggedIn, approvalRefreshNonce) {
                    if (!isLoggedIn || uid == null) {
                        approvalLoading = false
                        accountStatus = null
                        planStatus = null
                        trialEndAtMs = null
                        subscriptionEndAtMs = null
                        onDispose { }
                    } else {
                        approvalLoading = true
                        val docRef = firestore.collection("users").document(uid)
                        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, _ ->
                            val now = System.currentTimeMillis()
                            if (snapshot != null && snapshot.exists()) {
                                val status = snapshot.getString("status") ?: "pending"
                                val currentPlan = snapshot.getString("planStatus") ?: "trial"
                                val currentTrialEnd = anyToMillis(snapshot.get("trialEndAt"))
                                val currentSubEnd = anyToMillis(snapshot.get("subscriptionEndAt"))

                                accountStatus = status
                                planStatus = currentPlan
                                trialEndAtMs = currentTrialEnd
                                subscriptionEndAtMs = currentSubEnd
                                approvalLoading = false

                                val needsClientMeta = snapshot.get("phone") == null
                                if (needsClientMeta) {
                                    docRef.set(
                                        mapOf(
                                            "phone" to (phoneNumber ?: ""),
                                            "updatedAt" to now
                                        ),
                                        SetOptions.merge()
                                    )
                                }
                            } else {
                                accountStatus = "pending"
                                planStatus = null
                                trialEndAtMs = null
                                subscriptionEndAtMs = null
                                approvalLoading = false
                                // Create minimal doc; backend function will set protected access fields.
                                docRef.set(
                                    mapOf(
                                        "phone" to (phoneNumber ?: ""),
                                        "createdAt" to now,
                                        "updatedAt" to now
                                    ),
                                    SetOptions.merge()
                                )
                            }
                        }
                        onDispose { listener.remove() }
                    }
                }

                if (approvalLoading) {
                    AccessControlScreen(
                        title = "Checking Access",
                        message = "Please wait..."
                    )
                    return@ARSTokenTheme
                }

                val now = System.currentTimeMillis()
                val blocked = accountStatus == "blocked"
                val effectiveEndMs = when (planStatus) {
                    "active" -> subscriptionEndAtMs
                    "trial" -> trialEndAtMs
                    else -> trialEndAtMs
                }
                val expired = effectiveEndMs != null && now > effectiveEndMs
                val isApproved = accountStatus == "approved"
                val hasAccess = isApproved && !blocked && !expired
                if (!hasAccess) {
                    val message = when {
                        blocked -> "Your account is blocked by developer."
                        accountStatus == "pending" -> "Your account is pending approval/setup."
                        expired -> "Your trial/subscription has expired."
                        else -> "Your account access is not active yet."
                    }
                    AccessControlScreen(
                        title = "Access Restricted",
                        message = message,
                        onRefresh = {
                            approvalLoading = true
                            approvalRefreshNonce += 1
                        },
                        onSignOut = {
                            FirebaseAuth.getInstance().signOut()
                            isLoggedIn = false
                        }
                    )
                    return@ARSTokenTheme
                }

                val subscriptionLabel = buildSubscriptionLabel(
                    planStatus = planStatus,
                    trialEndAtMs = trialEndAtMs,
                    subscriptionEndAtMs = subscriptionEndAtMs
                )

                val syncEnabled = settingsState?.syncEnabled ?: true
                val syncHour = settingsState?.syncHour ?: 22
                val syncMinute = settingsState?.syncMinute ?: 0

                LaunchedEffect(isLoggedIn, uid, syncEnabled) {
                    if (!isLoggedIn || uid == null || !syncEnabled || lastLoginRefreshUid == uid) return@LaunchedEffect
                    try {
                        syncManager.refreshFromCloudOnLogin(uid)
                        lastLoginRefreshUid = uid
                    } catch (_: Exception) {
                    }
                }
                profileViewModel?.startSync { }

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

                when (currentScreen) {

                    AdminScreen.BILLING -> {
                        BillingScreen(
                            viewModel = billingViewModel,
                            businessName = businessName ?: "ARS Token",
                            businessPhone = businessPhone,
                            logoUrl = logoUrl,
                            openMenuOnLoad = openDrawerOnBilling,
                            menuHighlightOnAutoOpen = drawerHighlightOnOpen,
                            onMenuOpened = { openDrawerOnBilling = false },
                            onOpenReports = {
                                drawerHighlightOnOpen = AdminScreen.REPORTS
                                currentScreen = AdminScreen.REPORTS
                            },
                            onOpenCustomers = {
                                drawerHighlightOnOpen = AdminScreen.CUSTOMERS
                                currentScreen = AdminScreen.CUSTOMERS
                            },
                            onOpenItems = {
                                drawerHighlightOnOpen = AdminScreen.ITEMS
                                currentScreen = AdminScreen.ITEMS
                            },
                            onOpenSettings = {          // 👈 ADD
                                drawerHighlightOnOpen = AdminScreen.SETTINGS_LANDING
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
                                drawerHighlightOnOpen = AdminScreen.REPORTS
                                openDrawerOnBilling = true
                                currentScreen = AdminScreen.BILLING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
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
                                onDeleted = {
                                    currentScreen = AdminScreen.REPORTS
                                },
                                onBack = {
                                    currentScreen = AdminScreen.REPORTS
                                },
                                onHome = {
                                    currentScreen = AdminScreen.BILLING
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
                            onBack = {
                                drawerHighlightOnOpen = AdminScreen.CUSTOMERS
                                openDrawerOnBilling = true
                                currentScreen = AdminScreen.BILLING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
                            }
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
                                },
                                onHome = {
                                    currentScreen = AdminScreen.BILLING
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
                                drawerHighlightOnOpen = AdminScreen.ITEMS
                                openDrawerOnBilling = true
                                currentScreen = AdminScreen.BILLING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
                            }
                        )
                    }
                    AdminScreen.SETTINGS_LANDING -> {
                        SettingsLandingScreen(
                            onBack = {
                                drawerHighlightOnOpen = AdminScreen.SETTINGS_LANDING
                                openDrawerOnBilling = true
                                currentScreen = AdminScreen.BILLING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
                            },
                            onOpenBusinessProfile = {
                                currentScreen = AdminScreen.BUSINESS_PROFILE
                            },
                            onOpenPrintSettings = {
                                currentScreen = AdminScreen.PRINT_SETTINGS
                            },
                            onOpenBackupSettings = {
                                currentScreen = AdminScreen.BACKUP_SETTINGS
                            },
                            onSignOut = {
                                scope.launch {
                                    try {
                                        syncManager.clearLocalData()
                                    } catch (_: Exception) {
                                    }
                                    lastLoginRefreshUid = null
                                    FirebaseAuth.getInstance().signOut()
                                    currentScreen = AdminScreen.BILLING
                                }
                            },
                            settings = settingsState,
                            subscriptionLabel = subscriptionLabel
                        )
                    }
                    AdminScreen.BACKUP_SETTINGS -> {
                        BackupSettingsScreen(
                            settings = settingsState,
                            onBack = {
                                currentScreen = AdminScreen.SETTINGS_LANDING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
                            },
                            onSyncNow = {
                                if (uid != null && (settingsState?.syncEnabled == true)) {
                                    scope.launch {
                                        try {
                                            syncManager.syncAll(uid)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                            },
                            onSave = { enabled, hour, minute ->
                                val current = settingsState ?: StoreSettingsEntity(storeName = "My Store", phone = "")
                                settingsViewModel.save(
                                    current.copy(
                                        syncEnabled = enabled,
                                        syncHour = hour,
                                        syncMinute = minute
                                    )
                                )
                                if (uid != null) {
                                    scope.launch {
                                        try {
                                            syncManager.syncSettingsOnly(uid)
                                        } catch (_: Exception) {
                                            // Keep local setting if offline; cloud will catch up later.
                                        }
                                    }
                                }
                            }
                        )
                    }
                    AdminScreen.PRINT_SETTINGS -> {
                        PrintSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = {
                                currentScreen = AdminScreen.SETTINGS_LANDING
                            },
                            onHome = {
                                currentScreen = AdminScreen.BILLING
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
                                onHome = {
                                    currentScreen = AdminScreen.BILLING
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
                            onHome = {
                                currentScreen = AdminScreen.BILLING
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

@Composable
private fun AccessControlScreen(
    title: String,
    message: String,
    onRefresh: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val supportNumber = "+917799255011"
    val waLink = "https://wa.me/917799255011"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1518), Color(0xFF101C29), Color(0xFF0F1B2A))
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x1AFFFFFF)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFEAF2FF)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFEAF2FF),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Please contact the developer through WhatsApp: $supportNumber no calls please.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBFD1EA),
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFC2B4),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waLink))
                        runCatching { context.startActivity(intent) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1FAF5D)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Open WhatsApp Chat", color = Color.White)
                }

                if (onSignOut == null && onRefresh == null) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onRefresh?.invoke() },
                            modifier = Modifier
                                .background(Color(0x223B82F6), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh access status",
                                tint = Color(0xFFBFD1EA)
                            )
                        }
                        Button(
                            onClick = { onSignOut?.invoke() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3C55))
                        ) {
                            Text("Sign Out", color = Color(0xFFEAF2FF))
                        }
                    }
                }
            }
        }
    }
}

private fun anyToMillis(value: Any?): Long? {
    return when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Timestamp -> value.toDate().time
        is Date -> value.time
        else -> null
    }
}

private fun buildSubscriptionLabel(
    planStatus: String?,
    trialEndAtMs: Long?,
    subscriptionEndAtMs: Long?
): String {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return when (planStatus) {
        "active" -> {
            val endText = subscriptionEndAtMs?.let { df.format(Date(it)) } ?: "Not available"
            "Subscription ends on $endText"
        }
        "trial" -> {
            val endText = trialEndAtMs?.let { df.format(Date(it)) } ?: "Not available"
            "Trial ends on $endText"
        }
        "expired" -> {
            val endedAt = subscriptionEndAtMs ?: trialEndAtMs
            val endedText = endedAt?.let { df.format(Date(it)) } ?: "Not available"
            "Plan ended on $endedText"
        }
        else -> {
            val endText = (subscriptionEndAtMs ?: trialEndAtMs)
                ?.let { df.format(Date(it)) } ?: "Not available"
            "Plan ends on $endText"
        }
    }
}
