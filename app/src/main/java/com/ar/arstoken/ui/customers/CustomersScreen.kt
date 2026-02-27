package com.ar.arstoken.ui.customers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ar.arstoken.model.Customer
import com.ar.arstoken.viewmodel.CustomerViewModel
import androidx.activity.compose.BackHandler
import com.ar.arstoken.util.formatAmount

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomerViewModel,
    onCustomerSelected: (Customer) -> Unit,
    onBack: () -> Unit
) {
    val customers by viewModel.customersWithDue.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var phoneInput by remember { mutableStateOf("") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers") },
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
                .verticalScroll(rememberScrollState())
        ) {
            val fieldModifier = Modifier
                .widthIn(max = 520.dp)
                .heightIn(min = 48.dp)
            val actionButtonModifier = Modifier
                .widthIn(max = 240.dp)
                .heightIn(min = 48.dp)

            Spacer(Modifier.height(4.dp))

        // Add customer
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Customer name") },
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.addCustomer(name, phone)
                name = ""
                phone = ""
            },
            modifier = actionButtonModifier
        ) {
            Text("Add Customer")
        }

        Spacer(Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(Modifier.height(8.dp))

            // Customer list
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                customers.forEach { customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .combinedClickable(
                                onClick = { onCustomerSelected(customer) },
                                onLongClick = {
                                    editingCustomer = customer
                                    phoneInput = customer.phone
                                    showEditDialog = true
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text(customer.name) },
                            supportingContent = { Text("Due: ₹${formatAmount(customer.creditBalance)}") }
                        )
                    }
                }
            }
        }
    }
    BackHandler { onBack() }
    if (showEditDialog && editingCustomer != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Phone Number") },
            text = {
                Column {
                    Text(
                        text = editingCustomer!!.name,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateCustomerPhone(
                        customerId = editingCustomer!!.id,
                        phone = phoneInput
                    )
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
