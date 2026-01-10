package com.ar.arstoken.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.arstoken.model.Customer
import com.ar.arstoken.viewmodel.CustomerViewModel

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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Customers", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        // Add customer
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Customer name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.addCustomer(name, phone)
                name = ""
                phone = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Customer")
        }

        Spacer(Modifier.height(16.dp))

        Divider()

        Spacer(Modifier.height(8.dp))

        // Customer list
        LazyColumn {
            items(customers) { customer ->
                ListItem(
                    headlineContent = { Text(customer.name) },
                    supportingContent = { Text("Due: ₹${customer.creditBalance}") },
                    modifier = Modifier
                        .clickable { onCustomerSelected(customer) }
                        .combinedClickable(
                            onClick = { onCustomerSelected(customer) },
                            onLongClick = {
                                editingCustomer = customer
                                phoneInput = customer.phone
                                showEditDialog = true
                            }
                        )

                )
            }
        }
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
                            singleLine = true
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
}
