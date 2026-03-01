package com.ar.arstoken.ui.reports

import android.Manifest
import android.app.DatePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ar.arstoken.data.db.ItemSalesRow
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.util.ThermalPrinterHelper
import com.ar.arstoken.util.exportItemSummaryCsv
import com.ar.arstoken.util.exportItemSummaryPdf
import com.ar.arstoken.util.exportItemSummaryImage
import com.ar.arstoken.util.formatAmount
import com.ar.arstoken.util.formatQty
import com.ar.arstoken.util.shareFile
import com.ar.arstoken.viewmodel.ItemSalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class ReportTab {
    MENU,
    BILLS,
    ITEM_SUMMARY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemSalesReportScreen(
    viewModel: ItemSalesViewModel,
    onBack: () -> Unit,
    onSaleSelected: (Int) -> Unit,
    settings: StoreSettingsEntity,
    businessName: String?,
    businessPhone: String?
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF051012) else Color(0xFFF0F4F2)
    val topBarBg = if (isDark) Color(0xFF192327) else Color.White
    val topBarContent = if (isDark) Color(0xFFE8ECEF) else Color(0xFF202124)

    val sales by viewModel.sales.collectAsState<List<SaleEntity>>()
    val itemSales by viewModel.itemSales.collectAsState<List<ItemSalesRow>>()
    val fromDate by viewModel.fromDateMillis.collectAsState()
    val toDate by viewModel.toDateMillis.collectAsState()
    val activeCount = remember(sales) { sales.count { !it.isDeleted } }
    val deletedCount = remember(sales) { sales.count { it.isDeleted } }
    val formatter = rememberDateFormatter()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(ReportTab.MENU) }
    var pendingSaleId by remember { mutableStateOf<Int?>(null) }
    var pendingSummaryPrint by remember { mutableStateOf(false) }
    var billFilterMenuExpanded by remember { mutableStateOf(false) }
    var billFilterLabel by remember { mutableStateOf("Today") }

    val hasBtConnectPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                val saleId = pendingSaleId
                if (saleId != null) {
                    pendingSaleId = null
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
                if (pendingSummaryPrint) {
                    pendingSummaryPrint = false
                    val printText = formatItemSummaryPrint(
                        fromDate = fromDate,
                        toDate = toDate,
                        rows = itemSales,
                        settings = settings,
                        businessName = businessName
                    )
                    ThermalPrinterHelper().printToPairedPrinter(
                        text = printText,
                        options = ThermalPrinterHelper.PrintOptions(
                            bottomPaddingLines = settings.bottomPaddingLines,
                            spacingFix = settings.printerSpacingFix
                        )
                    )
                }
            }
        } else {
            pendingSaleId = null
            pendingSummaryPrint = false
        }
    }

    Scaffold(
        containerColor = screenBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg,
                    titleContentColor = topBarContent,
                    navigationIconContentColor = topBarContent
                ),
                title = {
                    Text(
                        when (selectedTab) {
                            ReportTab.MENU -> "Reports"
                            ReportTab.BILLS -> "Bill Report"
                            ReportTab.ITEM_SUMMARY -> "Item Report Summary"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedTab == ReportTab.MENU) onBack() else selectedTab = ReportTab.MENU
                        }
                    ) {
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (selectedTab == ReportTab.MENU) {
                ReportMenu(
                    onBillsClick = { selectedTab = ReportTab.BILLS },
                    onItemSummaryClick = { selectedTab = ReportTab.ITEM_SUMMARY }
                )
            } else if (selectedTab == ReportTab.BILLS) {
                Box {
                    FakeDropDownField(
                        text = billFilterLabel,
                        onClick = { billFilterMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = billFilterMenuExpanded,
                        onDismissRequest = { billFilterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Today") },
                            onClick = {
                                viewModel.setToday()
                                billFilterLabel = "Today"
                                billFilterMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Last Week") },
                            onClick = {
                                viewModel.setLast7Days()
                                billFilterLabel = "Last Week"
                                billFilterMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Last Month") },
                            onClick = {
                                viewModel.setLast30Days()
                                billFilterLabel = "Last Month"
                                billFilterMenuExpanded = false
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            if (selectedTab == ReportTab.BILLS) {
                BillReportContent(
                    sales = sales,
                    fromDate = fromDate,
                    toDate = toDate,
                    activeCount = activeCount,
                    deletedCount = deletedCount,
                    formatter = formatter,
                    onSaleSelected = onSaleSelected,
                    onFromDateChange = { picked ->
                        val normalized = startOfDay(picked)
                        val currentTo = toDate
                        val targetTo = if (normalized > currentTo) endOfDay(normalized) else currentTo
                        viewModel.setDateRange(normalized, targetTo)
                    },
                    onToDateChange = { picked ->
                        val normalized = endOfDay(picked)
                        val currentFrom = fromDate
                        val targetFrom = if (normalized < currentFrom) startOfDay(normalized) else currentFrom
                        viewModel.setDateRange(targetFrom, normalized)
                    },
                    onPrint = { saleId ->
                        if (hasBtConnectPermission) {
                            scope.launch {
                                val receipt = viewModel.buildReceipt(
                                    saleId,
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
                            pendingSaleId = saleId
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    }
                )
            } else if (selectedTab == ReportTab.ITEM_SUMMARY) {
                ItemSummaryContent(
                    itemSales = itemSales,
                    fromDate = fromDate,
                    toDate = toDate,
                    onExportCsv = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                exportItemSummaryCsv(context, fromDate, toDate, itemSales)
                            }
                            snackbarHostState.showSnackbar("CSV saved: ${file.name}")
                        }
                    },
                    onExportPdf = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                exportItemSummaryPdf(context, fromDate, toDate, itemSales)
                            }
                            shareFile(context, file, "application/pdf", "Share PDF")
                        }
                    },
                    onPrintPdf = {
                        scope.launch {
                            if (hasBtConnectPermission) {
                                val printText = formatItemSummaryPrint(
                                    fromDate = fromDate,
                                    toDate = toDate,
                                    rows = itemSales,
                                    settings = settings,
                                    businessName = businessName
                                )
                                ThermalPrinterHelper().printToPairedPrinter(
                                    text = printText,
                                    options = ThermalPrinterHelper.PrintOptions(
                                        bottomPaddingLines = settings.bottomPaddingLines,
                                        spacingFix = settings.printerSpacingFix
                                    )
                                )
                            } else {
                                pendingSummaryPrint = true
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }
                    },
                    onShareImage = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                exportItemSummaryImage(context, fromDate, toDate, itemSales)
                            }
                            shareFile(context, file, "image/png", "Share Report")
                        }
                    },
                    onToday = { viewModel.setToday() },
                    onLastWeek = {
                        val range = lastWeekRange()
                        viewModel.setDateRange(range.first, range.second)
                    },
                    onLastMonth = {
                        val range = lastMonthRange()
                        viewModel.setDateRange(range.first, range.second)
                    },
                    onCurrentFy = {
                        val range = currentFyRange()
                        viewModel.setDateRange(range.first, range.second)
                    },
                    onFromDateChange = { picked ->
                        val normalized = startOfDay(picked)
                        val currentTo = toDate
                        val targetTo = if (normalized > currentTo) endOfDay(normalized) else currentTo
                        viewModel.setDateRange(normalized, targetTo)
                    },
                    onToDateChange = { picked ->
                        val normalized = endOfDay(picked)
                        val currentFrom = fromDate
                        val targetFrom = if (normalized < currentFrom) startOfDay(normalized) else currentFrom
                        viewModel.setDateRange(targetFrom, normalized)
                    }
                )
            }
        }
    }
    BackHandler {
        if (selectedTab == ReportTab.MENU) onBack() else selectedTab = ReportTab.MENU
    }
}

@Composable
private fun ReportMenu(
    onBillsClick: () -> Unit,
    onItemSummaryClick: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBillsClick),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Bill Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "View all bills, deleted status, and print bill copies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onItemSummaryClick),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Item Report Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "See serial-wise item quantity sold and total amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillReportContent(
    sales: List<SaleEntity>,
    fromDate: Long,
    toDate: Long,
    activeCount: Int,
    deletedCount: Int,
    formatter: SimpleDateFormat,
    onSaleSelected: (Int) -> Unit,
    onFromDateChange: (Long) -> Unit,
    onToDateChange: (Long) -> Unit,
    onPrint: (Int) -> Unit
) {
    val context = LocalContext.current
    val inputDate = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val rangeFormat = remember { SimpleDateFormat("dd-MM-yy", Locale.getDefault()) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Active: $activeCount",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Deleted: $deletedCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FakeDropDownField(
            text = inputDate.format(Date(fromDate)),
            modifier = Modifier.weight(1f),
            onClick = {
                showDatePicker(
                    context = context,
                    initialTime = fromDate,
                    onPicked = onFromDateChange
                )
            }
        )
        FakeDropDownField(
            text = inputDate.format(Date(toDate)),
            modifier = Modifier.weight(1f),
            onClick = {
                showDatePicker(
                    context = context,
                    initialTime = toDate,
                    onPicked = onToDateChange
                )
            }
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF6E78E8)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bill Report",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${rangeFormat.format(Date(fromDate))} to ${rangeFormat.format(Date(toDate))}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(sales) { sale ->
            val isDeleted = sale.isDeleted
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSaleSelected(sale.id) },
                shape = RoundedCornerShape(10.dp),
                border = if (isDeleted) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                } else {
                    null
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isDeleted) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bill #${sale.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isDeleted) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("DELETED") },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
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
                    Text(
                        text = sale.customerName ?: "Cash Sale",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Total ₹${formatAmount(sale.totalAmount)} | Paid ₹${formatAmount(sale.paidAmount)} | Due ₹${formatAmount(sale.dueAmount)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isDeleted) {
                        sale.deletedAt?.let {
                            Text(
                                text = "Deleted: ${formatter.format(Date(it))}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = "Reason: ${sale.deleteReason ?: "-"}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { onPrint(sale.id) },
                        modifier = Modifier.widthIn(max = 120.dp)
                    ) {
                        Text("Print")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemSummaryContent(
    itemSales: List<ItemSalesRow>,
    fromDate: Long,
    toDate: Long,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onShareImage: () -> Unit,
    onToday: () -> Unit,
    onLastWeek: () -> Unit,
    onLastMonth: () -> Unit,
    onCurrentFy: () -> Unit,
    onFromDateChange: (Long) -> Unit,
    onToDateChange: (Long) -> Unit
) {
    val context = LocalContext.current
    val inputDate = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val headingDate = remember { SimpleDateFormat("dd-MM-yy", Locale.getDefault()) }
    val totalQty = remember(itemSales) { itemSales.sumOf { it.totalQty } }
    val totalAmount = remember(itemSales) { itemSales.sumOf { it.totalAmount } }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var selectedFilterLabel by remember { mutableStateOf("Today") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (itemSales.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactIconAction(
                    onClick = onExportCsv,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download CSV",
                            tint = Color(0xFF6E78E8)
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                CompactIconAction(
                    onClick = onExportPdf,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = Color(0xFF6E78E8)
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                CompactIconAction(
                    onClick = onPrintPdf,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print",
                            tint = Color(0xFF6E78E8)
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                CompactIconAction(
                    onClick = onShareImage,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share as image",
                            tint = Color(0xFF6E78E8)
                        )
                    }
                )
            }
        }

        Box {
            FakeDropDownField(
                text = selectedFilterLabel,
                onClick = { filterMenuExpanded = true }
            )
            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Today") },
                    onClick = {
                        onToday()
                        selectedFilterLabel = "Today"
                        filterMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Last Week") },
                    onClick = {
                        onLastWeek()
                        selectedFilterLabel = "Last Week"
                        filterMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Last Month") },
                    onClick = {
                        onLastMonth()
                        selectedFilterLabel = "Last Month"
                        filterMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(fyLabelFor(fromDate)) },
                    onClick = {
                        onCurrentFy()
                        selectedFilterLabel = fyLabelFor(fromDate)
                        filterMenuExpanded = false
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FakeDropDownField(
                text = inputDate.format(Date(fromDate)),
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(
                        context = context,
                        initialTime = fromDate,
                        onPicked = onFromDateChange
                    )
                }
            )
            FakeDropDownField(
                text = inputDate.format(Date(toDate)),
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(
                        context = context,
                        initialTime = toDate,
                        onPicked = onToDateChange
                    )
                }
            )
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF6E78E8)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Item Sale Report",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${headingDate.format(Date(fromDate))} to ${headingDate.format(Date(toDate))}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "Total Sale Quantity",
                value = formatQty(totalQty),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Sale Amount",
                value = formatAmount(totalAmount),
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    if (itemSales.isEmpty()) {
        Text("No items sold in selected period.")
        return
    }

    val headerBlue = Color(0xFF3D7BE0)
    val rowLight = Color(0xFFF6F8FC)
    val line = Color(0xFFD6DCE8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, line, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(headerBlue, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderText("Sr\nNo", modifier = Modifier.weight(0.7f))
        TableHeaderText("Item Name", modifier = Modifier.weight(1.9f))
        TableHeaderText("Item\nCategory", modifier = Modifier.weight(1.6f))
        TableHeaderText("Total Sale\nQuantity", modifier = Modifier.weight(1.8f))
        TableHeaderText("Total Sale\nAmount", modifier = Modifier.weight(1.8f))
    }

    LazyColumn {
        itemsIndexed(itemSales) { index, row ->
            val bg = if (index % 2 == 0) rowLight else Color.White
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, line)
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableCellText((index + 1).toString(), modifier = Modifier.weight(0.7f))
                TableCellText(
                    text = row.itemName,
                    modifier = Modifier.weight(1.9f),
                    maxLines = 2
                )
                TableCellText(
                    text = row.itemCategory?.takeIf { it.isNotBlank() } ?: "-",
                    modifier = Modifier.weight(1.6f)
                )
                TableCellText(
                    text = formatQty(row.totalQty),
                    modifier = Modifier.weight(1.8f)
                )
                TableCellText(
                    text = formatAmount(row.totalAmount),
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    }
}

@Composable
private fun FakeDropDownField(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val border = if (isDark) Color(0xFF3B3F57) else Color(0xFFC7C9D3)
    val fill = if (isDark) Color(0xFF0A1216) else Color(0xFFF9FAFB)
    val textColor = if (isDark) Color(0xFFE7E9EE) else Color(0xFF2D3134)
    Row(
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .background(fill, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = textColor)
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = textColor
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val titleColor = Color(0xFF596066)
    val valueColor = Color(0xFF1E2225)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF58C49C)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDFD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = titleColor
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = valueColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CompactIconAction(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val border = Color(0xFF6E78E8)
    val bg = if (isDark) Color(0xFF132028) else Color(0xFFFFFFFF)
    Box(
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .background(bg, RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
        ) {
            icon()
        }
    }
}

@Composable
private fun TableHeaderText(
    text: String,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun TableCellText(
    text: String,
    modifier: Modifier,
    maxLines: Int = 1
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatItemSummaryPrint(
    fromDate: Long,
    toDate: Long,
    rows: List<ItemSalesRow>,
    settings: StoreSettingsEntity,
    businessName: String?
): String {
    val width = settings.charactersPerLine.coerceIn(24, 64)
    val noCol = 3
    val qtyCol = 6
    val amtCol = 10
    val itemCol = (width - noCol - qtyCol - amtCol - 3).coerceAtLeast(8)

    fun line() = "-".repeat(width)
    fun fit(text: String, w: Int): String = if (text.length >= w) text.take(w) else text.padEnd(w, ' ')
    fun fitRight(text: String, w: Int): String = if (text.length >= w) text.takeLast(w) else text.padStart(w, ' ')
    fun dateText(ms: Long): String = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ms))
    fun lr(left: String, right: String): String {
        val spaces = (width - left.length - right.length).coerceAtLeast(1)
        return (left + " ".repeat(spaces) + right).take(width)
    }

    val totalQty = rows.sumOf { it.totalQty }
    val totalAmount = rows.sumOf { it.totalAmount }
    val sb = StringBuilder()
    sb.appendLine("{C}{B}${(businessName ?: settings.storeName.ifBlank { "ARS TOKEN" }).uppercase()}{/B}")
    sb.appendLine("{C}{B}ITEM SALE REPORT{/B}")
    sb.appendLine("{C}${dateText(fromDate)} to ${dateText(toDate)}")
    sb.appendLine("{L}${line()}")
    sb.appendLine("{L}" + fit("No", noCol) + " " + fit("Item", itemCol) + " " + fitRight("Qty", qtyCol) + " " + fitRight("Amount", amtCol))
    sb.appendLine("{L}${line()}")

    rows.forEachIndexed { index, row ->
        sb.appendLine(
            "{L}" +
                fit((index + 1).toString(), noCol) + " " +
                fit(row.itemName, itemCol) + " " +
                fitRight(formatQty(row.totalQty), qtyCol) + " " +
                fitRight(formatAmount(row.totalAmount), amtCol)
        )
    }

    sb.appendLine("{L}${line()}")
    sb.appendLine("{L}" + lr("Total Qty", formatQty(totalQty)))
    sb.appendLine("{L}" + lr("Total Amount", formatAmount(totalAmount)))
    sb.appendLine("{L}${line()}")
    return sb.toString()
}

private fun showDatePicker(
    context: android.content.Context,
    initialTime: Long,
    onPicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
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

private fun fyLabelFor(fromDateMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = fromDateMillis }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val fyStartYear = if (month >= 4) year else year - 1
    val fyEndYear = (fyStartYear + 1) % 100
    val fyStartShort = fyStartYear % 100
    return "FY(${fyStartShort.toString().padStart(2, '0')}-$fyEndYear)"
}

private fun lastWeekRange(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val end = endOfDay(now)
    val start = startOfDay(now - (6L * 24L * 60L * 60L * 1000L))
    return start to end
}

private fun lastMonthRange(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val end = endOfDay(now)
    val start = startOfDay(now - (29L * 24L * 60L * 60L * 1000L))
    return start to end
}

private fun currentFyRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val fyStartYear = if (month >= 4) year else year - 1
    val startCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, fyStartYear)
        set(Calendar.MONTH, Calendar.APRIL)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return startOfDay(startCal.timeInMillis) to endOfDay(System.currentTimeMillis())
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }
}
