package com.ar.arstoken.ui.billing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.ar.arstoken.model.Item

@Composable
fun EditItemSheet(
    item: Item,
    initialQty: Int,
    initialTotalPrice: Int,
    onConfirm: (Int, Int) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val unitPrice = item.price

    var quantityText by remember {
        mutableStateOf(initialQty.toString())
    }
    var priceText by remember {
        mutableStateOf(initialTotalPrice.toString())
    }

    var lastEdited by remember { mutableStateOf(EditField.NONE) }
    val qty = quantityText.toIntOrNull() ?: 0
    val price = priceText.toIntOrNull() ?: 0

    val isValid = qty > 0 && price > 0


    LaunchedEffect(lastEdited, quantityText, priceText) {
        when (lastEdited) {
            EditField.QUANTITY -> {
                val qty = quantityText.toIntOrNull() ?: return@LaunchedEffect
                priceText = (qty * unitPrice).toString()
            }
            EditField.PRICE -> {
                val price = priceText.toIntOrNull() ?: return@LaunchedEffect
                if (unitPrice > 0) {
                    quantityText = (price / unitPrice).toString()
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()          // 👈 handles keyboard
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {


    Text(
            text = item.name,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = {
                quantityText = it
                    lastEdited = EditField.QUANTITY
            },
            label = { Text("Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = priceText,
            onValueChange = {
                priceText = it
                lastEdited = EditField.PRICE
            },
            label = { Text("Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val qty = quantityText.toIntOrNull() ?: 0
        val totalPrice = priceText.toIntOrNull() ?: 0

        val isValid = qty > 0 && totalPrice > 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Discard")
            }

            Button(
                onClick = {
                    onConfirm(qty, totalPrice)
                },
                enabled = isValid
            ) {
                Text("OK")
            }

        }

    }
}

private enum class EditField {
    QUANTITY,
    PRICE,
    NONE
}
