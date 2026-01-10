package com.ar.arstoken.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ar.arstoken.ui.customers.LedgerRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.ar.arstoken.model.SaleType
import com.ar.arstoken.util.salesToCsv
import com.ar.arstoken.util.shareText


@Composable
fun CustomerLedgerScreen(
    customerName: String,
    customerPhone: String,
    viewModel: CustomerLedgerViewModel,
    onBack: () -> Unit
) {
    // 1️⃣ Collect sales FIRST
    val sales by viewModel.sales.collectAsState<List<SaleEntity>>()

    // 2️⃣ Date filter state
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }

    // 3️⃣ FILTERED SALES (THIS WAS MISSING / MISPLACED)
    val filteredSales = remember(sales, fromDate, toDate) {
        sales.filter { sale ->
            val ts = sale.timestamp
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

     Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(customerName, style = MaterialTheme.typography.titleLarge)

            Row {
                TextButton(onClick = onBack) {
                    Text("Back")
                }

                Spacer(Modifier.width(8.dp))

                Button(onClick = { showPaymentDialog = true }) {
                    Text("Receive Payment")
                }

                if (customerPhone.isNotBlank()) {
                    Button(onClick = {
                        val totalDue = ledgerRows.lastOrNull()?.second ?: 0.0

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
                    }) {
                        Text("WhatsApp")
                    }
                }


                Button(onClick = {
                    val csv = salesToCsv(
                        customerName = customerName,
                        sales = filteredSales   // respects date filter
                    )

                    shareText(
                        context = context,
                        text = csv,
                        title = "Ledger - $customerName"
                    )
                }) {
                    Text("Export")
                }

            }
        }
        val totalDue = ledgerRows.lastOrNull()?.second ?: 0.0

        Text(
            text = "Total Due: ₹$totalDue",
            style = MaterialTheme.typography.titleMedium,
            color = if (totalDue > 0)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(12.dp))

        // Filter buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                fromDate = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                toDate = cal.timeInMillis
            }) {
                Text("Today")
            }

            OutlinedButton(onClick = {
                fromDate = null
                toDate = null
            }) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Ledger list
        LazyColumn {
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
                Divider()
            }
        }
    }
    if (showPaymentDialog) {
        val currentDue = ledgerRows.lastOrNull()?.second ?: 0.0
        val enteredAmount = paymentAmount.toDoubleOrNull() ?: 0.0
        val remainingDue = (currentDue - enteredAmount).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Receive Payment") },
            text = {
                Column {
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount received") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // ✅ THIS IS THE EXACT PLACE
                    Text(
                        text = "Remaining due: ₹$remainingDue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingDue == 0.0)
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
                    val amount = paymentAmount.toDoubleOrNull()
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
private fun LedgerRow(
    sale: SaleEntity,
    runningBalance: Double
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
): List<Pair<SaleEntity, Double>> {

    var balance = 0.0

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