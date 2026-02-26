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
    onAddCategory: () -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var categoryExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var editedPrice by remember { mutableStateOf("") }
    var editedCategory by remember { mutableStateOf<String?>(null) }
    var editCategoryExpanded by remember { mutableStateOf(false) }
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
            value = viewModel.draftItemName,
            onValueChange = viewModel::updateDraftName,
            label = { Text("Item name") },
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.draftItemPrice,
            onValueChange = viewModel::updateDraftPrice,
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = fieldModifier,
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = viewModel.draftItemCategory ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                modifier = fieldModifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add Category") },
                    onClick = {
                        categoryExpanded = false
                        onAddCategory()
                    }
                )
                DropdownMenuItem(
                    text = { Text("No Category") },
                    onClick = {
                        viewModel.updateDraftCategory(null)
                        categoryExpanded = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            viewModel.updateDraftCategory(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.addItem()
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
                            supportingContent = {
                                Text(
                                    "₹${item.price}" +
                                        (item.category?.let { " • $it" } ?: "")
                                )
                            },
                            trailingContent = {
                                Row {
                                    TextButton(
                                        onClick = {
                                            selectedItem = item
                                            editedPrice = item.price.toString()
                                            editedCategory = item.category
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
                Column {
                    OutlinedTextField(
                        value = editedPrice,
                        onValueChange = { editedPrice = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = editCategoryExpanded,
                        onExpandedChange = { editCategoryExpanded = !editCategoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = editedCategory ?: "No Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = editCategoryExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = editCategoryExpanded,
                            onDismissRequest = { editCategoryExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Category") },
                                onClick = {
                                    editedCategory = null
                                    editCategoryExpanded = false
                                }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        editedCategory = category
                                        editCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePrice(
                            selectedItem!!.id,
                            editedPrice.toIntOrNull() ?: selectedItem!!.price
                        )
                        viewModel.assignCategory(
                            selectedItem!!.id,
                            editedCategory
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
