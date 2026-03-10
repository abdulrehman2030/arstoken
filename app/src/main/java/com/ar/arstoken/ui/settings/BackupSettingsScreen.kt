package com.ar.arstoken.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.ui.components.BottomLeftBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    settings: StoreSettingsEntity?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSyncNow: () -> Unit,
    onSave: (enabled: Boolean, hour24: Int, minute: Int) -> Unit
) {
    val current = settings ?: StoreSettingsEntity(storeName = "My Store", phone = "")
    var enabled by remember(current) { mutableStateOf(current.syncEnabled) }
    val initialHour12 = remember(current) { hour12(current.syncHour) }
    val initialAmPm = remember(current) { if (current.syncHour < 12) "AM" else "PM" }
    var selectedHour by remember(current) { mutableStateOf(initialHour12) }
    var selectedMinute by remember(current) { mutableStateOf(current.syncMinute.coerceIn(0, 59)) }
    var amPm by remember(current) { mutableStateOf(initialAmPm) }

    LaunchedEffect(enabled, selectedHour, selectedMinute, amPm) {
        onSave(enabled, toHour24(selectedHour, amPm), selectedMinute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup Settings") },
                navigationIcon = {
                    IconButton(onClick = onHome) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (enabled) "ON" else "OFF",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Switch(
                                checked = enabled,
                                onCheckedChange = { enabled = it }
                            )
                        }
                    }
                }

                Text(
                    text = "Daily Sync Time",
                    style = MaterialTheme.typography.titleMedium
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NumberSelector(
                            label = "Hour",
                            options = (1..12).toList(),
                            selected = selectedHour,
                            onSelect = { selectedHour = it },
                            modifier = Modifier.weight(1f),
                            enabled = enabled
                        )
                        NumberSelector(
                            label = "Minute",
                            options = (0..59).toList(),
                            selected = selectedMinute,
                            format = { "%02d".format(it) },
                            onSelect = { selectedMinute = it },
                            modifier = Modifier.weight(1f),
                            enabled = enabled
                        )
                        StringSelector(
                            label = "AM/PM",
                            options = listOf("AM", "PM"),
                            selected = amPm,
                            onSelect = { amPm = it },
                            modifier = Modifier.weight(1f),
                            enabled = enabled
                        )
                    }
                }

                Text(
                    text = "Sync Now",
                    style = MaterialTheme.typography.titleMedium
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Button(
                        onClick = onSyncNow,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(8.dp)
                    ) {
                        Text("Sync Now")
                    }
                }
            }
            BottomLeftBackButton(
                onBack = onBack,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }

    BackHandler { onBack() }
}

@Composable
private fun NumberSelector(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(format(selected))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = { Text(format(value)) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StringSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        ) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun hour12(hour24: Int): Int {
    val h = hour24 % 12
    return if (h == 0) 12 else h
}

private fun toHour24(hour12: Int, amPm: String): Int {
    return when {
        amPm == "AM" && hour12 == 12 -> 0
        amPm == "AM" -> hour12
        amPm == "PM" && hour12 == 12 -> 12
        else -> hour12 + 12
    }
}
