package com.ar.arstoken.ui.billing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import com.ar.arstoken.model.Customer
import com.ar.arstoken.model.PaymentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    total: Int,
    paymentMode: PaymentMode,
    partialPaidAmount: Int,
    onPartialAmountChange: (String) -> Unit,
    onCustomerSelected: (Customer?) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onSummary: () -> Unit
) {
    val isPartialValid = paymentMode != PaymentMode.PARTIAL ||
        (partialPaidAmount > 0 && partialPaidAmount < total)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Bill Options", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Customer", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCustomer?.name ?: "None",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select customer") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onCustomerSelected(null)
                        expanded = false
                    }
                )

                customers.forEach { customer ->
                    DropdownMenuItem(
                        text = { Text(customer.name) },
                        onClick = {
                            onCustomerSelected(customer)
                            expanded = false
                        }
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Text("Payment Type", style = MaterialTheme.typography.titleMedium)

        PaymentRadio(
            label = "Cash",
            selected = paymentMode == PaymentMode.CASH
        ) { onPaymentModeChange(PaymentMode.CASH) }

        PaymentRadio(
            label = "Credit",
            selected = paymentMode == PaymentMode.CREDIT,
            enabled = selectedCustomer != null
        ) { onPaymentModeChange(PaymentMode.CREDIT) }

        PaymentRadio(
            label = "Partial",
            selected = paymentMode == PaymentMode.PARTIAL,
            enabled = selectedCustomer != null
        ) { onPaymentModeChange(PaymentMode.PARTIAL) }

        if (paymentMode == PaymentMode.PARTIAL) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = if (partialPaidAmount == 0) "" else partialPaidAmount.toString(),
                onValueChange = onPartialAmountChange,
                label = { Text("Paid Amount") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPartialValid) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Partial amount must be greater than 0 and less than total (₹$total).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSummary,
            enabled = isPartialValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Summary")
        }
    }
}

@Composable
private fun PaymentRadio(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
    }
}
