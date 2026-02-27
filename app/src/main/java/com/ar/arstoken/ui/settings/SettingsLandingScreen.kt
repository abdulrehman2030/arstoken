package com.ar.arstoken.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ar.arstoken.data.db.StoreSettingsEntity
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLandingScreen(
    onBack: () -> Unit,
    onOpenPrintSettings: () -> Unit,
    onOpenBusinessProfile: () -> Unit,
    onSignOut: () -> Unit,
    settings: StoreSettingsEntity?,
    onSyncNow: () -> Unit,
    onSaveSyncTime: (hour: Int, minute: Int) -> Unit
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    val current = settings ?: StoreSettingsEntity(storeName = "My Store", phone = "")
    var hourInput by remember(current) { mutableStateOf(current.syncHour.toString()) }
    var minuteInput by remember(current) { mutableStateOf(current.syncMinute.toString()) }
    val syncLabel = "%02d:%02d".format(current.syncHour, current.syncMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard(
                title = "Business Profile",
                subtitle = "Business name and phone",
                onClick = onOpenBusinessProfile
            )

            SettingsCard(
                title = "Print Settings",
                subtitle = "Receipt format and printer behavior",
                onClick = onOpenPrintSettings
            )

            SettingsCard(
                title = "Backup Settings",
                subtitle = "Sync now or schedule daily at $syncLabel",
                onClick = { showBackupDialog = true }
            )

            SettingsCard(
                title = "Sign Out",
                subtitle = "Log out of this device",
                onClick = { showSignOutDialog = true }
            )
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to verify your phone again to log in.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOut()
                }) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Settings") },
            text = { Text("Choose to sync now or change the daily backup time.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    onSyncNow()
                }) {
                    Text("Sync now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    showSyncDialog = true
                }) {
                    Text("Change time")
                }
            }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = { hourInput = it },
                        label = { Text("Hour (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                    OutlinedTextField(
                        value = minuteInput,
                        onValueChange = { minuteInput = it },
                        label = { Text("Minute (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hour = hourInput.toIntOrNull() ?: current.syncHour
                    val minute = minuteInput.toIntOrNull() ?: current.syncMinute
                    val safeHour = hour.coerceIn(0, 23)
                    val safeMinute = minute.coerceIn(0, 59)
                    onSaveSyncTime(safeHour, safeMinute)
                    showSyncDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    BackHandler {
        onBack()
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
