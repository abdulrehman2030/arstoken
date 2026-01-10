package com.ar.arstoken

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.ar.arstoken.data.db.AppDatabase
import com.ar.arstoken.data.repository.*
import com.ar.arstoken.model.Customer
import com.ar.arstoken.ui.billing.BillingScreen
import com.ar.arstoken.ui.customers.CustomerLedgerScreen
import com.ar.arstoken.ui.customers.CustomersScreen
import com.ar.arstoken.ui.items.ItemsScreen
import com.ar.arstoken.ui.reports.ItemSalesReportScreen
import com.ar.arstoken.ui.reports.ReportScreen
import com.ar.arstoken.ui.theme.ARSTokenTheme
import com.ar.arstoken.viewmodel.*

enum class AdminScreen {
    BILLING,
    REPORTS,
    CUSTOMERS,
    ITEMS,
    CUSTOMER_LEDGER

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
                var currentScreen by remember {
                    mutableStateOf(AdminScreen.BILLING)
                }

                // ----------------------------
                // Database (single instance)
                // ----------------------------
                val db = remember {
                    AppDatabase.get(applicationContext)
                }

                // ----------------------------
                // Repositories (single instance)
                // ----------------------------
                val saleRepo = remember { RoomSaleRepository(db) }
                val reportRepo: RoomReportRepository = RoomReportRepository(db)

                val reportViewModel = remember {
                    ReportViewModel(reportRepo)
                }

                val customerRepo = remember { RoomCustomerRepository(db) }
                val itemRepo = remember { RoomItemRepository(db) }
                var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
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
                        customerRepository = customerRepo
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
                            }
                        )
                    }

                    AdminScreen.REPORTS -> {
                        val vm = ItemSalesViewModel(reportRepo)

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
                                selectedCustomer = it
                                currentScreen = AdminScreen.CUSTOMER_LEDGER
                            },
                            onBack = { currentScreen = AdminScreen.BILLING }
                        )
                    }

                    AdminScreen.CUSTOMER_LEDGER -> {
                        val customer = selectedCustomer
                        if (customer != null) {
                            val ledgerVm = remember(customer.id) {
                                CustomerLedgerViewModel(
                                    customerId = customer.id,
                                    saleRepository = saleRepo
                                )
                            }

                            CustomerLedgerScreen(
                                customerName = customer.name,
                                customerPhone = customer.phone,
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
                        val itemViewModel = remember {
                            ItemViewModel(itemRepo)
                        }

                        ItemsScreen(
                            viewModel = itemViewModel,
                            onBack = {
                                currentScreen = AdminScreen.BILLING
                            }
                        )
                    }
                }
            }
        }
    }
}
