package com.ar.arstoken.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.arstoken.viewmodel.BusinessProfileViewModel
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    viewModel: BusinessProfileViewModel,
    phoneNumber: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    var businessName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profile) {
        val current = profile
        if (current != null) {
            businessName = current.businessName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fieldModifier = Modifier.widthIn(min = 280.dp, max = 520.dp)

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business name") },
                singleLine = true,
                modifier = fieldModifier
            )

            OutlinedTextField(
                value = phoneNumber.orEmpty(),
                onValueChange = {},
                label = { Text("Phone number") },
                singleLine = true,
                enabled = false,
                modifier = fieldModifier
            )

            TextButton(
                onClick = {
                    errorMessage = null
                    if (businessName.isBlank()) {
                        errorMessage = "Business name is required."
                        return@TextButton
                    }
                    viewModel.saveProfile(
                        businessName = businessName,
                        logoUrl = null,
                        phone = phoneNumber,
                        onError = { message -> errorMessage = message }
                    )
                    viewModel.startSync { }
                    scope.launch {
                        snackbarHostState.showSnackbar("Business name saved")
                    }
                    scope.launch {
                        delay(1400)
                        onSaved()
                    }
                },
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                Text("Save")
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    BackHandler {
        onBack()
    }

}
