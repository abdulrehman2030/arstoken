package com.ar.arstoken.print

import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.model.CartItem
import java.text.SimpleDateFormat
import java.util.*

object ReceiptFormatter {

    fun format(
        sale: SaleEntity,
        items: List<CartItem>
    ): String {

        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()

        sb.appendln("      ARS STORE")
        sb.appendln("----------------------------")
        sb.appendln("Date: ${df.format(Date(sale.timestamp))}")
        sb.appendln("Customer: ${sale.customerName}")
        sb.appendln("----------------------------")

        items.forEach {
            sb.appendln(
                "${it.item.name}  ${it.qty} x ${it.item.price} = ${
                    String.format("%.2f", it.qty * it.item.price)
                }"
            )
        }

        sb.appendln("----------------------------")
        sb.appendln("TOTAL : ₹${sale.totalAmount}")
        sb.appendln("PAID  : ₹${sale.paidAmount}")
        sb.appendln("DUE   : ₹${sale.dueAmount}")
        sb.appendln("MODE  : ${sale.saleType}")
        sb.appendln("----------------------------")
        sb.appendln("Thank you!")
        sb.appendln("\n\n")

        return sb.toString()
    }
}
