package com.ar.arstoken.ui.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ar.arstoken.data.db.ItemEntity
import com.ar.arstoken.viewmodel.ItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var editedPrice by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Items") },
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
        val fieldModifier = Modifier
            .widthIn(max = 520.dp)
            .heightIn(min = 48.dp)
        val actionButtonModifier = Modifier
            .widthIn(max = 240.dp)
            .heightIn(min = 48.dp)

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item name") },
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.addItem(name, price.toDoubleOrNull() ?: 0.0)
                name = ""
                price = ""
            },
            modifier = actionButtonModifier
        ) {
            Text("Add Item")
        }

        Spacer(Modifier.height(16.dp))

        HorizontalDivider()

            LazyColumn {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text("₹${item.price}") },
                            trailingContent = {
                                Row {
                                    TextButton(
                                        onClick = {
                                            selectedItem = item
                                            editedPrice = item.price.toString()
                                            showEditDialog = true
                                        }
                                    ) {
                                        Text("Edit")
                                    }

                                    TextButton(
                                        onClick = {
                                            itemToDelete = item
                                            showDeleteDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    if (showEditDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Price") },
            text = {
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = { editedPrice = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePrice(
                            selectedItem!!.id,
                            editedPrice.toDoubleOrNull() ?: selectedItem!!.price
                        )
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete ${itemToDelete!!.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(itemToDelete!!.id)
                        showDeleteDialog = false
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}
