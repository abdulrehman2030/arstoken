package com.ar.arstoken.ui.customers

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.viewmodel.CustomerLedgerViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.ar.arstoken.model.SaleType
import com.ar.arstoken.util.salesToCsv
import com.ar.arstoken.util.shareText
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    customerName: String,
    customerPhone: String,
    viewModel: CustomerLedgerViewModel,
    onBack: () -> Unit
) {
    val buttonTextSize = 10.sp

    // 1️⃣ Collect sales FIRST
    val sales by viewModel.sales.collectAsState<List<SaleEntity>>()

    // 2️⃣ Date filter state
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }
    var selectedMode by remember { mutableStateOf<SaleType?>(null) }
    var showFilterScreen by remember { mutableStateOf(false) }

    // 3️⃣ FILTERED SALES (THIS WAS MISSING / MISPLACED)
    val filteredSales = remember(sales, fromDate, toDate, selectedMode) {
        sales.filter { sale ->
            val ts = sale.timestamp
            val modeOk = selectedMode == null || sale.saleType == selectedMode!!.name
            modeOk &&
                (fromDate == null || ts >= fromDate!!) &&
                (toDate == null || ts <= toDate!!)
        }
    }

    // 4️⃣ Running balance on FILTERED list
     val ledgerRows = remember(filteredSales) {
         computeRunningBalances(filteredSales.reversed())
     }
     val context = LocalContext.current
     var showPaymentDialog by remember { mutableStateOf(false) }
     var paymentAmount by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showFilterScreen) {
        CustomerLedgerFilterScreen(
            initialFromDate = fromDate,
            initialToDate = toDate,
            initialMode = selectedMode,
            onApply = { newFrom, newTo, mode ->
                fromDate = newFrom
                toDate = newTo
                selectedMode = mode
                showFilterScreen = false
            },
            onCancel = { showFilterScreen = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(customerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val totalDue = ledgerRows.lastOrNull()?.second ?: 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (totalDue > 0) {
                            showPaymentDialog = true
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Total due is ₹$totalDue, can't receive payment."
                                )
                            }
                        }
                    }
                ) {
                    Text("Receive Payment", fontSize = buttonTextSize)
                }

                if (customerPhone.isNotBlank()) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val totalDue = ledgerRows.lastOrNull()?.second ?: 0

                            val message = """
                                Hi $customerName,
                                Your pending balance at ARS Store is ₹$totalDue.
                                Please clear it at your convenience.
                            """.trimIndent()

                            openWhatsApp(
                                context = context,
                                phone = customerPhone,
                                message = message
                            )
                        }
                    ) {
                        Text("WhatsApp", fontSize = buttonTextSize)
                    }
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val csv = salesToCsv(
                            customerName = customerName,
                            sales = filteredSales
                        )

                        shareText(
                            context = context,
                            text = csv,
                            title = "Ledger - $customerName"
                        )
                    }
                ) {
                    Text("Export", fontSize = buttonTextSize)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Total Due: ₹$totalDue",
                style = MaterialTheme.typography.titleMedium,
                color = if (totalDue > 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { showFilterScreen = true }) {
                    Text("Filter", fontSize = buttonTextSize)
                }

                OutlinedButton(onClick = {
                    fromDate = null
                    toDate = null
                    selectedMode = null
                }) {
                    Text("Clear", fontSize = buttonTextSize)
                }
            }

            val filterSummary = remember(fromDate, toDate, selectedMode) {
                buildFilterSummary(fromDate, toDate, selectedMode)
            }
            if (filterSummary != "No active filters") {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = filterSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (ledgerRows.isEmpty()) {
                    item {
                        Text(
                            "No transactions found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(items = ledgerRows) { (sale, balance) ->
                    LedgerRow(
                        sale = sale,
                        runningBalance = balance
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    if (showPaymentDialog) {
        val currentDue = ledgerRows.lastOrNull()?.second ?: 0
        val enteredAmount = paymentAmount.toIntOrNull() ?: 0
        val remainingDue = (currentDue - enteredAmount).coerceAtLeast(0)

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Receive Payment") },
            text = {
                Column {
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount received") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // ✅ THIS IS THE EXACT PLACE
                    Text(
                        text = "Remaining due: ₹$remainingDue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingDue == 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = enteredAmount > 0 && enteredAmount <= currentDue,
                    onClick = {
                    val amount = paymentAmount.toIntOrNull()
                    if (amount != null && amount > 0 && amount <= currentDue) {
                        viewModel.receivePayment(amount)
                    }
                    paymentAmount = ""
                    showPaymentDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    paymentAmount = ""
                    showPaymentDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }


}

@Composable
private fun CustomerLedgerFilterScreen(
    initialFromDate: Long?,
    initialToDate: Long?,
    initialMode: SaleType?,
    onApply: (Long?, Long?, SaleType?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val displayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var fromDate by remember { mutableStateOf(initialFromDate) }
    var toDate by remember { mutableStateOf(initialToDate) }
    var mode by remember { mutableStateOf(initialMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ledger Filters", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onCancel) { Text("Back") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Payment Mode", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        ModeOption(label = "Any", selected = mode == null, onClick = { mode = null })
        ModeOption(label = "Cash", selected = mode == SaleType.CASH, onClick = { mode = SaleType.CASH })
        ModeOption(label = "Credit", selected = mode == SaleType.CREDIT, onClick = { mode = SaleType.CREDIT })
        ModeOption(label = "Partial", selected = mode == SaleType.PARTIAL, onClick = { mode = SaleType.PARTIAL })
        ModeOption(label = "Payment Received", selected = mode == SaleType.PAYMENT, onClick = { mode = SaleType.PAYMENT })

        Spacer(Modifier.height(16.dp))
        Text("Date Range", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showDatePicker(
                    context = context,
                    initialTime = fromDate,
                    onPicked = { picked -> fromDate = startOfDay(picked) }
                )
            }
        ) {
            Text(fromDate?.let { "From: ${displayFormat.format(Date(it))}" } ?: "Select From Date")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showDatePicker(
                    context = context,
                    initialTime = toDate,
                    onPicked = { picked -> toDate = endOfDay(picked) }
                )
            }
        ) {
            Text(toDate?.let { "To: ${displayFormat.format(Date(it))}" } ?: "Select To Date")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                fromDate = null
                toDate = null
            }
        ) {
            Text("Clear Dates")
        }

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onApply(
                    fromDate,
                    toDate,
                    mode
                )
            }
        ) {
            Text("Apply Filters")
        }
    }
}

@Composable
private fun ModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        RadioButton(selected = selected, onClick = onClick)
    }
}

private fun showDatePicker(
    context: android.content.Context,
    initialTime: Long?,
    onPicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        if (initialTime != null) {
            timeInMillis = initialTime
        }
    }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            val picked = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }.timeInMillis
            onPicked(picked)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun startOfDay(time: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfDay(time: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun buildFilterSummary(
    fromDate: Long?,
    toDate: Long?,
    mode: SaleType?
): String {
    val parts = mutableListOf<String>()
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    if (mode != null) {
        parts.add("Mode: ${mode.name}")
    }
    if (fromDate != null || toDate != null) {
        val fromText = fromDate?.let { df.format(Date(it)) } ?: "Any"
        val toText = toDate?.let { df.format(Date(it)) } ?: "Any"
        parts.add("Date: $fromText to $toText")
    }
    return if (parts.isEmpty()) "No active filters" else parts.joinToString(" | ")
}

@Composable
private fun LedgerRow(
    sale: SaleEntity,
    runningBalance: Int
) {
    val date = remember(sale.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            .format(Date(sale.timestamp))
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(date, style = MaterialTheme.typography.labelSmall)
//        Text("Bill: ₹${sale.totalAmount}")
//        Text("Paid: ₹${sale.paidAmount}")
        Text("Due: ₹${sale.totalAmount - sale.paidAmount}")
        val isPayment = sale.saleType == SaleType.PAYMENT.name

        Text(
            text = "Mode: ${sale.saleType}",
            color = if (isPayment)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (isPayment)
                "Payment: ₹${sale.paidAmount}"
            else
                "Bill: ₹${sale.totalAmount}"
        )


        Text(
            "Balance: ₹$runningBalance",
            color = if (runningBalance > 0)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )
    }
}

private fun computeRunningBalances(
    sales: List<SaleEntity>
): List<Pair<SaleEntity, Int>> {

    var balance = 0

    return sales.map { sale ->
        balance += (sale.totalAmount - sale.paidAmount)
        sale to balance
    }
}

fun openWhatsApp(
    context: android.content.Context,
    phone: String,
    message: String
) {
    val url = "https://wa.me/91$phone?text=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    context.startActivity(intent)
}
