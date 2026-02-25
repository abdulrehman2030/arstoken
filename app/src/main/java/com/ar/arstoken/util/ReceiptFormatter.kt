package com.ar.arstoken.util

import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import java.text.SimpleDateFormat
import java.util.*

fun formatReceipt(
    storeName: String,
    phone: String,
    sale: SaleEntity,
    items: List<SaleItemEntity>
): String {

    val date = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        .format(Date(sale.timestamp))

    val sb = StringBuilder()

    sb.appendLine(storeName.uppercase())
    sb.appendLine("Phone: $phone")
    sb.appendLine("------------------------------")
    sb.appendLine("Bill No: ${sale.id}")
    sb.appendLine("Date: $date")
    sb.appendLine("Bill To: ${sale.customerName}")
    sb.appendLine("------------------------------")
    sb.appendLine("Item      Qty   Rate  Total")

    items.forEach {
        sb.appendLine(
            "${it.itemName.take(10)}  ${it.quantity}  ${it.unitPrice}  ${it.totalPrice}"
        )
    }

    sb.appendLine("------------------------------")
    sb.appendLine("Sub Total: ${sale.totalAmount}")
    sb.appendLine("Paid: ${sale.paidAmount}")
    sb.appendLine("Due: ${sale.dueAmount}")
    sb.appendLine("Mode: ${sale.saleType}")
    sb.appendLine("------------------------------")
    sb.appendLine("Thank you!")

    return sb.toString()
}
