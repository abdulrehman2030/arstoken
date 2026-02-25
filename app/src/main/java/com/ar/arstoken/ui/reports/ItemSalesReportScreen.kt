package com.ar.arstoken.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ar.arstoken.data.db.ItemSalesRow
import com.ar.arstoken.viewmodel.ItemSalesViewModel

@Composable
fun ItemSalesReportScreen(
    viewModel: ItemSalesViewModel,
    onBack: () -> Unit
) {
    // ✅ FIXED: delegation works now
    val items by viewModel.items.collectAsState<List<ItemSalesRow>>()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = { viewModel.setToday() }) {
                Text("Today")
            }
            TextButton(onClick = { viewModel.setThisWeek() }) {
                Text("This Week")
            }
            TextButton(onClick = { viewModel.setThisMonth() }) {
                Text("This Month")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Item Sales", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) {
                Text("Back")
            }

        }

        Spacer(Modifier.height(8.dp))

        LazyColumn {
            // ✅ FIXED: correct LazyColumn items()
            items(items) { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.itemName, fontWeight = FontWeight.Bold)
                        Text("Qty: ${row.totalQty}")
                        Text("Amount: ₹${row.totalAmount}")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
