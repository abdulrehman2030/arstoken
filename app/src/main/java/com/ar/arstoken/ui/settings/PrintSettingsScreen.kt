package com.ar.arstoken.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.viewmodel.SettingsViewModel
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val current by viewModel.settings.collectAsState()
    val settings = current ?: StoreSettingsEntity(storeName = "My Store", phone = "")

    var primaryPrinterEnabled by remember(settings) { mutableStateOf(settings.primaryPrinterEnabled) }
    var printerType by remember(settings) { mutableStateOf(settings.printerType) }
    var autoPrintSale by remember(settings) { mutableStateOf(settings.autoPrintSale) }

    var charactersPerLine by remember(settings) { mutableStateOf(settings.charactersPerLine.toString()) }
    var column2Chars by remember(settings) { mutableStateOf(settings.column2Chars.toString()) }
    var column3Chars by remember(settings) { mutableStateOf(settings.column3Chars.toString()) }
    var column4Chars by remember(settings) { mutableStateOf(settings.column4Chars.toString()) }

    var printTokenNumber by remember(settings) { mutableStateOf(settings.printTokenNumber) }
    var businessNameSize by remember(settings) { mutableStateOf(settings.businessNameSize) }
    var totalAmountSize by remember(settings) { mutableStateOf(settings.totalAmountSize) }
    var printCreatedInfo by remember(settings) { mutableStateOf(settings.printCreatedInfo) }
    var printFooter by remember(settings) { mutableStateOf(settings.printFooter) }
    var printerSpacingFix by remember(settings) { mutableStateOf(settings.printerSpacingFix) }
    var bottomPaddingLines by remember(settings) { mutableStateOf(settings.bottomPaddingLines.toString()) }
    var printItemMultiLine by remember(settings) { mutableStateOf(settings.printItemMultiLine) }
    var itemOrder by remember(settings) { mutableStateOf(settings.itemOrder) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Print Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Primary Printer", style = MaterialTheme.typography.titleMedium)

            SwitchRow(
                title = "Primary Printer Enabled",
                checked = primaryPrinterEnabled,
                onCheckedChange = { primaryPrinterEnabled = it }
            )

            DropdownRow(
                label = "Printer Type",
                value = printerType,
                options = listOf("BLUETOOTH", "USB"),
                onValueChange = { printerType = it }
            )

            SwitchRow(
                title = "Auto Print Sale",
                checked = autoPrintSale,
                onCheckedChange = { autoPrintSale = it }
            )

            NumberField(
                label = "Characters Per Line",
                value = charactersPerLine,
                onValueChange = { charactersPerLine = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumberField(
                    label = "Column 2",
                    value = column2Chars,
                    onValueChange = { column2Chars = it },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "Column 3",
                    value = column3Chars,
                    onValueChange = { column3Chars = it },
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    label = "Column 4",
                    value = column4Chars,
                    onValueChange = { column4Chars = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Bill Content", style = MaterialTheme.typography.titleMedium)

            SwitchRow(
                title = "Print Token Number",
                checked = printTokenNumber,
                onCheckedChange = { printTokenNumber = it }
            )

            DropdownRow(
                label = "Business Name Size",
                value = businessNameSize,
                options = listOf("SMALL", "MEDIUM", "LARGE"),
                onValueChange = { businessNameSize = it }
            )

            DropdownRow(
                label = "Total Amount Size",
                value = totalAmountSize,
                options = listOf("SMALL", "MEDIUM", "LARGE"),
                onValueChange = { totalAmountSize = it }
            )

            SwitchRow(
                title = "Print Created Info",
                checked = printCreatedInfo,
                onCheckedChange = { printCreatedInfo = it }
            )

            SwitchRow(
                title = "Print Item Multi Line",
                checked = printItemMultiLine,
                onCheckedChange = { printItemMultiLine = it }
            )

            DropdownRow(
                label = "Item Order",
                value = itemOrder,
                options = listOf("INSERTED", "ALPHABETICAL"),
                onValueChange = { itemOrder = it }
            )

            OutlinedTextField(
                value = printFooter,
                onValueChange = { printFooter = it },
                label = { Text("Print Footer") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 3
            )

            Text("Spacing", style = MaterialTheme.typography.titleMedium)

            SwitchRow(
                title = "Printer Spacing Fix",
                checked = printerSpacingFix,
                onCheckedChange = { printerSpacingFix = it }
            )

            NumberField(
                label = "Bottom Padding Lines (0-10)",
                value = bottomPaddingLines,
                onValueChange = { bottomPaddingLines = it }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .height(48.dp),
                onClick = {
                    val updated = settings.copy(
                        primaryPrinterEnabled = primaryPrinterEnabled,
                        printerType = printerType,
                        autoPrintSale = autoPrintSale,
                        charactersPerLine = charactersPerLine.toIntOrNull()?.coerceIn(24, 64) ?: 32,
                        column2Chars = column2Chars.toIntOrNull()?.coerceIn(2, 8) ?: 4,
                        column3Chars = column3Chars.toIntOrNull()?.coerceIn(3, 10) ?: 6,
                        column4Chars = column4Chars.toIntOrNull()?.coerceIn(3, 10) ?: 6,
                        printTokenNumber = printTokenNumber,
                        businessNameSize = businessNameSize,
                        totalAmountSize = totalAmountSize,
                        printCreatedInfo = printCreatedInfo,
                        printFooter = printFooter,
                        printerSpacingFix = printerSpacingFix,
                        bottomPaddingLines = bottomPaddingLines.toIntOrNull()?.coerceIn(0, 10) ?: 1,
                        printItemMultiLine = printItemMultiLine,
                        itemOrder = itemOrder
                    )

                    viewModel.save(updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Saved successfully")
                    }
                    onSaved()
                }
            ) {
                Text("Save")
            }
        }
    }

    BackHandler {
        onBack()
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}
