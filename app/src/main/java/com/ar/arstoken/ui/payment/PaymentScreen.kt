package com.ar.arstoken.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ar.arstoken.viewmodel.BillingViewModel

enum class PaymentType {
    CASH, UPI, CREDIT, PARTIAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: BillingViewModel,
    onPaymentComplete: () -> Unit
) {
    val total = viewModel.getTotal()
    var selectedPayment by remember { mutableStateOf(PaymentType.CASH) }
    var paidAmount by remember { mutableStateOf("") }

    val paid = paidAmount.toDoubleOrNull() ?: 0.0
    val due = (total - paid).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()          // 👈 handles keyboard
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {


        Text(
                text = "Total: ₹$total",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            PaymentOption(
                label = "Cash",
                selected = selectedPayment == PaymentType.CASH
            ) { selectedPayment = PaymentType.CASH }

            PaymentOption(
                label = "UPI",
                selected = selectedPayment == PaymentType.UPI
            ) { selectedPayment = PaymentType.UPI }

            PaymentOption(
                label = "Credit",
                selected = selectedPayment == PaymentType.CREDIT
            ) { selectedPayment = PaymentType.CREDIT }

            PaymentOption(
                label = "Partial",
                selected = selectedPayment == PaymentType.PARTIAL
            ) { selectedPayment = PaymentType.PARTIAL }

            if (selectedPayment == PaymentType.PARTIAL) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = { paidAmount = it },
                    label = { Text("Paid Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Due: ₹$due")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPaymentComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Complete Bill")
            }
        }
    }
}
