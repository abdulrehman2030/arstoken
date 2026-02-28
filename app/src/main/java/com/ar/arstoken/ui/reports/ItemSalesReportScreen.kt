package com.ar.arstoken.ui.reports

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.util.ThermalPrinterHelper
import com.ar.arstoken.util.formatAmount
import com.ar.arstoken.viewmodel.ItemSalesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSalesReportScreen(
    viewModel: ItemSalesViewModel,
    onBack: () -> Unit,
    onSaleSelected: (Int) -> Unit,
    settings: StoreSettingsEntity,
    businessName: String?,
    businessPhone: String?
) {
    val sales by viewModel.sales.collectAsState<List<SaleEntity>>()
    val activeCount = remember(sales) { sales.count { !it.isDeleted } }
    val deletedCount = remember(sales) { sales.count { it.isDeleted } }
    val formatter = rememberDateFormatter()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSaleId by remember { mutableStateOf<Int?>(null) }

    val hasBtConnectPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val saleId = pendingSaleId ?: return@rememberLauncherForActivityResult
            pendingSaleId = null
            scope.launch {
                val receipt = viewModel.buildReceipt(saleId, businessName, businessPhone)
                    ?: return@launch
                ThermalPrinterHelper().printToPairedPrinter(
                    text = receipt,
                    options = ThermalPrinterHelper.PrintOptions(
                        bottomPaddingLines = settings.bottomPaddingLines,
                        spacingFix = settings.printerSpacingFix
                    )
                )
            }
        } else {
            pendingSaleId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Report") },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { viewModel.setToday() }) {
                    Text("Today")
                }
                TextButton(onClick = { viewModel.setThisWeek() }) {
                    Text("This Week")
                }
                TextButton(onClick = { viewModel.setThisMonth() }) {
                    Text("This Month")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Active Bills: $activeCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Deleted: $deletedCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(sales) { sale ->
                    val isDeleted = sale.isDeleted
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSaleSelected(sale.id) },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isDeleted) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                        } else {
                            null
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDeleted) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bill #${sale.id}", fontWeight = FontWeight.Bold)
                                if (isDeleted) {
                                    AssistChip(
                                        onClick = {},
                                        enabled = false,
                                        label = { Text("DELETED") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            disabledContainerColor = MaterialTheme.colorScheme.error,
                                            disabledLabelColor = MaterialTheme.colorScheme.onError
                                        )
                                    )
                                }
                            }
                            Text(
                                text = formatter.format(Date(sale.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Customer: ${sale.customerName ?: "Cash Sale"}")
                            Text("Mode: ${sale.saleType}")
                            Text("Total: ₹${formatAmount(sale.totalAmount)}")
                            Text("Paid: ₹${formatAmount(sale.paidAmount)}")
                            Text("Due: ₹${formatAmount(sale.dueAmount)}")
                            if (isDeleted) {
                                sale.deletedAt?.let {
                                    Text(
                                        text = "Deleted On: ${formatter.format(Date(it))}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "Reason: ${sale.deleteReason ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    if (hasBtConnectPermission) {
                                        scope.launch {
                                            val receipt = viewModel.buildReceipt(
                                                sale.id,
                                                businessName,
                                                businessPhone
                                            ) ?: return@launch
                                            ThermalPrinterHelper().printToPairedPrinter(
                                                text = receipt,
                                                options = ThermalPrinterHelper.PrintOptions(
                                                    bottomPaddingLines = settings.bottomPaddingLines,
                                                    spacingFix = settings.printerSpacingFix
                                                )
                                            )
                                        }
                                    } else {
                                        pendingSaleId = sale.id
                                        bluetoothPermissionLauncher.launch(
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        )
                                    }
                                },
                                modifier = Modifier.widthIn(max = 160.dp)
                            ) {
                                Text("Print")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    BackHandler { onBack() }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }
}
