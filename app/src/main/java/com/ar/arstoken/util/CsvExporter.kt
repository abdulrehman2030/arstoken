package com.ar.arstoken.util

import com.ar.arstoken.data.db.SaleEntity
import java.text.SimpleDateFormat
import java.util.*

fun salesToCsv(
    customerName: String,
    sales: List<SaleEntity>
): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    val header = "Date,Total,Paid,Due,Mode\n"

    val rows = sales.joinToString("\n") { sale ->
        val date = sdf.format(Date(sale.timestamp))
        val due = sale.totalAmount - sale.paidAmount
        "$date,${sale.totalAmount},${sale.paidAmount},$due,${sale.saleType}"
    }

    return "Customer,$customerName\n\n$header$rows"
}
