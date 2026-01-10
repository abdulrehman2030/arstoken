package com.ar.arstoken.ui.billing

import androidx.compose.foundation.layout.*
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
import com.ar.arstoken.model.Customer
import com.ar.arstoken.model.PaymentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    paymentMode: PaymentMode,
    partialPaidAmount: Double,
    onPartialAmountChange: (String) -> Unit,
    onCustomerSelected: (Customer?) -> Unit,
    onPaymentModeChange: (PaymentMode) -> Unit,
    onSummary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // -----------------
        // Customer section
        // -----------------
        Text("Customer", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }

        Text("Customer", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

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

        // -----------------
        // Payment section
        // -----------------
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
                value = if (partialPaidAmount == 0.0) "" else partialPaidAmount.toString(),
                onValueChange = onPartialAmountChange,
                label = { Text("Paid Amount") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSummary,
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
        modifier = Modifier.fillMaxWidth(),
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
