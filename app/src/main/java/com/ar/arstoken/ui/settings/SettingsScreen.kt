package com.ar.arstoken.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.ar.arstoken.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var storeName by remember(settings) {
        mutableStateOf(settings?.storeName ?: "")
    }
    var phone by remember(settings) {
        mutableStateOf(settings?.phone ?: "")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Settings") },
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
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            val fieldModifier = Modifier
                .widthIn(max = 520.dp)
                .heightIn(min = 48.dp)
            val actionButtonModifier = Modifier
                .widthIn(max = 240.dp)
                .heightIn(min = 48.dp)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Store Name") },
                modifier = fieldModifier,
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = fieldModifier,
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = actionButtonModifier,
                onClick = {
                    viewModel.save(storeName, phone)

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
}
