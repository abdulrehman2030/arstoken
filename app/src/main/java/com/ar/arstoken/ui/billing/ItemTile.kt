package com.ar.arstoken.ui.billing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTile(
    name: String,
    price: Double,
    quantityInCart: Double?,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(90.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quantityInCart != null)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "₹$price",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quantity badge
            if (quantityInCart != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "%.2f".format(quantityInCart).trimEnd('0').trimEnd('.'),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

}
