package com.ar.arstoken.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.arstoken.data.db.StoreSettingsEntity
import androidx.activity.compose.BackHandler
import com.ar.arstoken.ui.components.BottomLeftBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLandingScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenPrintSettings: () -> Unit,
    onOpenBusinessProfile: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onSignOut: () -> Unit,
    settings: StoreSettingsEntity?,
    subscriptionLabel: String
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCard(
                    title = "Subscription",
                    subtitle = subscriptionLabel,
                    onClick = {}
                )

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
                    subtitle = "Manage auto backup schedule and sync now",
                    onClick = onOpenBackupSettings
                )

                SettingsCard(
                    title = "Sign Out",
                    subtitle = "Log out of this device",
                    onClick = { showSignOutDialog = true }
                )
            }
            BottomLeftBackButton(
                onBack = onBack,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart)
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
