package com.ar.arstoken.util

import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import com.ar.arstoken.util.formatAmount
import com.ar.arstoken.util.formatQty
import java.text.SimpleDateFormat
import java.util.*

fun formatReceipt(
    settings: StoreSettingsEntity,
    businessNameOverride: String?,
    businessPhoneOverride: String?,
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    headerNote: String? = null
): String {
    val paperWidth = settings.charactersPerLine.coerceIn(24, 64)
    val date = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        .format(Date(sale.timestamp))
    val customerText = when {
        sale.customerName.isNullOrBlank() -> "Cash Sale"
        sale.customerName.equals("Retail", ignoreCase = true) -> "Cash Sale"
        else -> sale.customerName
    }

    val serialCol = 2
    val qtyCol = settings.column2Chars.coerceIn(2, 8)
    val rateCol = settings.column3Chars.coerceIn(3, 10)
    val totalCol = settings.column4Chars.coerceIn(3, 10)
    val itemCol = (paperWidth - serialCol - qtyCol - rateCol - totalCol - 4).coerceAtLeast(8)

    fun dashed() = "-".repeat(paperWidth)
    fun leftRight(left: String, right: String): String {
        val l = left.trim()
        val r = right.trim()
        val spaces = (paperWidth - l.length - r.length).coerceAtLeast(1)
        return (l + " ".repeat(spaces) + r).takeLast(paperWidth)
    }
    fun money(v: Double): String = formatAmount(v)
    fun qty(v: Double): String = formatQty(v)
    fun fit(text: String, width: Int): String =
        if (text.length >= width) text.take(width) else text.padEnd(width, ' ')
    fun fitRight(text: String, width: Int): String =
        if (text.length >= width) text.takeLast(width) else text.padStart(width, ' ')
    fun compact(v: Double): String = formatQty(v)

    val sb = StringBuilder()

    // Header
    val normalizedName = businessNameOverride
        ?.takeIf { it.isNotBlank() }
        ?: settings.storeName.ifBlank { "ARS TOKEN" }
    val headerName = normalizedName.uppercase()
    val storeHeading = when (settings.businessNameSize) {
        "LARGE" -> "{C}{B}{W2}$headerName{/B}"
        "SMALL" -> "{C}$headerName"
        else -> "{C}{B}$headerName{/B}"
    }
    sb.appendLine(storeHeading)
    if (!headerNote.isNullOrBlank()) {
        sb.appendLine("{C}{B}$headerNote{/B}")
    }
    val phoneLine = businessPhoneOverride?.takeIf { it.isNotBlank() }
        ?: settings.phone.takeIf { it.isNotBlank() }
    if (!phoneLine.isNullOrBlank()) {
        sb.appendLine("{C}Phone: $phoneLine")
    }
    sb.appendLine("{L}${dashed()}")
    sb.appendLine("{L}Bill No: ${sale.id}")
    if (settings.printTokenNumber) {
        sb.appendLine("{L}Token No: ${sale.id}")
    }
    if (settings.printCreatedInfo) {
        sb.appendLine("{L}Created On: $date")
    }
    sb.appendLine("{L}Bill To: $customerText")
    sb.appendLine("{L}${dashed()}")

    // Table header
    sb.appendLine(
        "{L}" + fit("No", serialCol) + " " +
            fit("Item", itemCol) + " " +
            fitRight("Qty", qtyCol) + " " +
            fitRight("Rate", rateCol) + " " +
            fitRight("Total", totalCol)
    )
    sb.appendLine("{L}${dashed()}")

    // Table rows with wrapped item name
    var totalQty = 0.0
    items.forEachIndexed { itemIndex, row ->
        totalQty += row.quantity
        val nameParts = if (settings.printItemMultiLine) {
            row.itemName.chunked(itemCol).ifEmpty { listOf("") }
        } else {
            listOf(row.itemName.take(itemCol))
        }
        nameParts.forEachIndexed { index, part ->
            if (index == 0) {
                sb.appendLine(
                    "{L}" + fit((itemIndex + 1).toString(), serialCol) + " " +
                        fit(part, itemCol) + " " +
                        fitRight(compact(row.quantity), qtyCol) + " " +
                        fitRight(row.unitPrice.toString(), rateCol) + " " +
                        fitRight(money(row.totalPrice), totalCol)
                )
            } else {
                sb.appendLine("{L}" + fit("", serialCol) + " " + fit(part, itemCol))
            }
        }
    }

    sb.appendLine("{L}${dashed()}")
    sb.appendLine(leftRight("Total Items:", items.size.toString()))
    sb.appendLine(leftRight("Total Quantity:", qty(totalQty)))
    sb.appendLine(leftRight("Sub Total", money(sale.totalAmount)))
    when (settings.totalAmountSize) {
        "LARGE" -> sb.appendLine("{C}{B}{W2}Total ${money(sale.totalAmount)}{/B}")
        "MEDIUM" -> sb.appendLine("{C}{B}Total ${money(sale.totalAmount)}{/B}")
    }
    sb.appendLine(leftRight("Mode of Payment", sale.saleType.lowercase(Locale.getDefault())))
    sb.appendLine(leftRight("Received", money(sale.paidAmount)))
    sb.appendLine("{L}${dashed()}")
    val footer = settings.printFooter.ifBlank { "Thank You! Visit Again!" }
    sb.appendLine("{C}$footer")

    return sb.toString()
}
