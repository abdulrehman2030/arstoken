package com.ar.arstoken.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.ar.arstoken.data.db.ItemSalesRow
import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportItemSummaryCsv(
    context: Context,
    fromDate: Long,
    toDate: Long,
    rows: List<ItemSalesRow>
): File {
    val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
    val outFile = File(outDir, "item_report_$timeTag.csv")

    val csv = buildString {
        append("Item Sale Report\n")
        append("From,${SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(fromDate))}\n")
        append("To,${SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(toDate))}\n")
        append("\n")
        append("Sr No,Item Name,Item Category,Total Sale Quantity,Total Sale Amount\n")
        rows.forEachIndexed { index, row ->
            append(index + 1)
            append(',')
            append(csvEscape(row.itemName))
            append(',')
            append(csvEscape(row.itemCategory ?: "-"))
            append(',')
            append(formatQty(row.totalQty))
            append(',')
            append(formatAmount(row.totalAmount))
            append('\n')
        }
    }

    outFile.writeText(csv)
    return outFile
}

fun exportItemSummaryImage(
    context: Context,
    fromDate: Long,
    toDate: Long,
    rows: List<ItemSalesRow>
): File {
    val width = 1440
    val rowHeight = 64
    val headerHeight = 68
    val topSpace = 220
    val bottomSpace = 40
    val tableRows = rows.take(60)
    val height = topSpace + headerHeight + (tableRows.size * rowHeight) + bottomSpace

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 34f
    }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3D7BE0") }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D6DCE8")
        strokeWidth = 2f
    }
    val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#202124")
        textSize = 24f
    }
    val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F6F8FC") }

    val df = SimpleDateFormat("dd-MM-yy", Locale.US)
    canvas.drawText("Item Sale Report", 40f, 70f, headingPaint)
    canvas.drawText(
        "${df.format(Date(fromDate))} to ${df.format(Date(toDate))}",
        40f,
        120f,
        subtitlePaint
    )

    val totalQty = rows.sumOf { it.totalQty }
    val totalAmount = rows.sumOf { it.totalAmount }
    canvas.drawText("Total Qty: ${formatQty(totalQty)}", 40f, 175f, subtitlePaint)
    canvas.drawText("Total Amount: ${formatAmount(totalAmount)}", 520f, 175f, subtitlePaint)

    val col = intArrayOf(0, 120, 520, 780, 1080, width)
    var y = topSpace
    canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + headerHeight).toFloat(), headerPaint)
    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
    canvas.drawLine(0f, (y + headerHeight).toFloat(), width.toFloat(), (y + headerHeight).toFloat(), linePaint)
    for (i in 0 until col.size) {
        canvas.drawLine(col[i].toFloat(), y.toFloat(), col[i].toFloat(), (y + headerHeight).toFloat(), linePaint)
    }
    canvas.drawText("Sr", 40f, y + 42f, headerTextPaint)
    canvas.drawText("Item Name", 140f, y + 42f, headerTextPaint)
    canvas.drawText("Category", 540f, y + 42f, headerTextPaint)
    canvas.drawText("Qty", 820f, y + 42f, headerTextPaint)
    canvas.drawText("Amount", 1110f, y + 42f, headerTextPaint)
    y += headerHeight

    tableRows.forEachIndexed { index, row ->
        if (index % 2 == 0) {
            canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + rowHeight).toFloat(), altRowPaint)
        }
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)
        for (i in 0 until col.size) {
            canvas.drawLine(col[i].toFloat(), y.toFloat(), col[i].toFloat(), (y + rowHeight).toFloat(), linePaint)
        }
        canvas.drawText((index + 1).toString(), 40f, y + 42f, rowTextPaint)
        canvas.drawText(row.itemName.take(22), 140f, y + 42f, rowTextPaint)
        canvas.drawText((row.itemCategory ?: "-").take(12), 540f, y + 42f, rowTextPaint)
        canvas.drawText(formatQty(row.totalQty), 820f, y + 42f, rowTextPaint)
        canvas.drawText(formatAmount(row.totalAmount), 1110f, y + 42f, rowTextPaint)
        y += rowHeight
    }
    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), linePaint)

    val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outFile = File(outDir, "item_report_$timeTag.png")
    FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return outFile
}

fun exportItemSummaryPdf(
    context: Context,
    fromDate: Long,
    toDate: Long,
    rows: List<ItemSalesRow>
): File {
    val imageFile = exportItemSummaryImage(context, fromDate, toDate, rows)
    val bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
    val page = pdf.startPage(pageInfo)
    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
    pdf.finishPage(page)

    val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outFile = File(outDir, "item_report_$timeTag.pdf")
    FileOutputStream(outFile).use { pdf.writeTo(it) }
    pdf.close()
    bitmap.recycle()
    return outFile
}

fun shareFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

fun shareFileToWhatsApp(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        `package` = "com.whatsapp"
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            shareFile(context, file, mimeType, chooserTitle)
        }
}

fun exportCustomerSalesPdf(
    context: Context,
    storeName: String?,
    customerName: String,
    fromDate: Long,
    toDate: Long,
    sales: List<SaleEntity>,
    saleItemsBySaleId: Map<Int, List<SaleItemEntity>>
): File {
    val pageWidth = 842
    val pageHeight = 1191
    val margin = 32f
    val lineHeight = 22f
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F4FBF")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val storeNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111111")
        textSize = 15f
    }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F4FBF")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val billHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAF1FF")
        style = Paint.Style.FILL
    }
    val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F6FC")
        style = Paint.Style.FILL
    }
    val rowAltBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAFAFA")
        style = Paint.Style.FILL
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C7D3EB")
        strokeWidth = 1.6f
    }
    val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B7C6E4")
        strokeWidth = 1.4f
    }
    val netDuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0C2454")
        textSize = 19f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val df = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val rowDate = SimpleDateFormat("dd MMM yy HH:mm", Locale.getDefault())
    val ordered = sales.sortedBy { it.timestamp }

    val pdf = PdfDocument()
    var pageNo = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
    var canvas = page.canvas
    var y = margin
    val colItem = margin + 8f
    val colQty = 470f
    val colPrice = 560f
    val colAmount = 660f
    val tableLeft = margin
    val tableRight = pageWidth - margin
    val colQtyStart = 450f
    val colPriceStart = 540f
    val colAmountStart = 640f

    fun drawHeader() {
        y = margin
        val centerX = pageWidth / 2f
        canvas.drawText("ApexCounter", centerX, y, appNamePaint)
        y += 30f
        val storeTitle = storeName?.takeIf { it.isNotBlank() } ?: "-"
        canvas.drawText(storeTitle, centerX, y, storeNamePaint)
        y += 30f
        canvas.drawText("Customer Ledger Report", margin, y, titlePaint)
        y += 26f
        canvas.drawText("Customer: $customerName", margin, y, bodyPaint)
        y += 18f
        canvas.drawText("From: ${df.format(Date(fromDate))}   To: ${df.format(Date(toDate))}", margin, y, bodyPaint)
        y += 22f
        canvas.drawLine(margin, y, pageWidth - margin, y, bodyPaint)
        y += 16f
    }

    drawHeader()

    fun nextPage() {
        pdf.finishPage(page)
        pageNo += 1
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        canvas = page.canvas
        drawHeader()
    }

    fun paymentLabel(sale: SaleEntity): String {
        return when (sale.saleType.uppercase(Locale.getDefault())) {
            "CASH" -> "Cash"
            "CREDIT" -> "Credit"
            "PARTIAL" -> "Partial"
            else -> sale.saleType
        }
    }

    if (ordered.isEmpty()) {
        canvas.drawText("No records in the selected date range.", margin, y, bodyPaint)
        y += lineHeight
    }

    fun drawBillTableHeader() {
        val top = y - 14f
        val bottom = y + 10f
        canvas.drawRect(tableLeft, top, tableRight, bottom, tableHeaderBgPaint)
        canvas.drawText("Item Name", colItem, y, headerPaint)
        canvas.drawText("Qty", colQty, y, headerPaint)
        canvas.drawText("Price", colPrice, y, headerPaint)
        canvas.drawText("Amount", colAmount, y, headerPaint)
        canvas.drawLine(tableLeft, top, tableRight, top, tableBorderPaint)
        canvas.drawLine(tableLeft, bottom, tableRight, bottom, tableBorderPaint)
        canvas.drawLine(tableLeft, top, tableLeft, bottom, tableBorderPaint)
        canvas.drawLine(colQtyStart, top, colQtyStart, bottom, tableBorderPaint)
        canvas.drawLine(colPriceStart, top, colPriceStart, bottom, tableBorderPaint)
        canvas.drawLine(colAmountStart, top, colAmountStart, bottom, tableBorderPaint)
        canvas.drawLine(tableRight, top, tableRight, bottom, tableBorderPaint)
        y += 24f
    }

    ordered.forEach { sale ->
        val items = saleItemsBySaleId[sale.id].orEmpty()
        val isPaymentEntry = sale.saleType.equals("PAYMENT", ignoreCase = true) || items.isEmpty()

        if (y > pageHeight - margin - 170f) {
            nextPage()
        }

        if (isPaymentEntry) {
            canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 16f, billHeaderBgPaint)
            canvas.drawText("Payment Received  |  ${rowDate.format(Date(sale.timestamp))}", margin, y, headerPaint)
            y += 20f
            canvas.drawText("Ref #${sale.id}  |  Amount: ${formatAmount(sale.paidAmount)}", margin, y, bodyPaint)
            y += 18f
            canvas.drawText("Updated Due: ${formatAmount(sale.dueAmount)}", margin, y, bodyPaint)
            y += 20f
        } else {
            canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 16f, billHeaderBgPaint)
            canvas.drawText("Bill #${sale.id}  |  ${rowDate.format(Date(sale.timestamp))}", margin, y, headerPaint)
            y += 18f

            val payment = paymentLabel(sale)
            val paymentLine = "Type: $payment  |  Paid: ${formatAmount(sale.paidAmount)}  |  Due: ${formatAmount(sale.dueAmount)}"
            canvas.drawText(paymentLine, margin, y, bodyPaint)
            y += 18f

            canvas.drawText("Bill Total: ${formatAmount(sale.totalAmount)}", margin, y, bodyPaint)
            y += 22f
            drawBillTableHeader()

            items.forEachIndexed { rowIndex, item ->
                if (y > pageHeight - margin - 24f) {
                    nextPage()
                    canvas.drawRect(margin, y - 14f, pageWidth - margin, y + 16f, billHeaderBgPaint)
                    canvas.drawText("Bill #${sale.id} (contd.)", margin, y, headerPaint)
                    y += 22f
                    drawBillTableHeader()
                }
                val rowTop = y - 14f
                val rowBottom = y + 8f
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, rowAltBgPaint)
                }
                canvas.drawText(item.itemName.take(44), colItem, y, bodyPaint)
                canvas.drawText(formatQty(item.quantity), colQty, y, bodyPaint)
                canvas.drawText(formatAmount(item.unitPrice.toDouble()), colPrice, y, bodyPaint)
                canvas.drawText(formatAmount(item.totalPrice), colAmount, y, bodyPaint)
                canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, tableBorderPaint)
                canvas.drawLine(tableLeft, rowTop, tableLeft, rowBottom, tableBorderPaint)
                canvas.drawLine(colQtyStart, rowTop, colQtyStart, rowBottom, tableBorderPaint)
                canvas.drawLine(colPriceStart, rowTop, colPriceStart, rowBottom, tableBorderPaint)
                canvas.drawLine(colAmountStart, rowTop, colAmountStart, rowBottom, tableBorderPaint)
                canvas.drawLine(tableRight, rowTop, tableRight, rowBottom, tableBorderPaint)
                y += lineHeight
            }
            y += 16f
        }
    }

    if (y > pageHeight - margin - 72f) {
        nextPage()
    }

    val billEntries = ordered.filter { saleItemsBySaleId[it.id].orEmpty().isNotEmpty() }
    val paymentEntries = ordered.filter { it.saleType.equals("PAYMENT", ignoreCase = true) || saleItemsBySaleId[it.id].orEmpty().isEmpty() }
    val grandTotal = billEntries.sumOf { it.totalAmount }
    val grandPaid = billEntries.sumOf { it.paidAmount }
    val grandDue = ordered.sumOf { it.dueAmount }
    val totalReceived = paymentEntries.sumOf { it.paidAmount }

    canvas.drawLine(margin, y, pageWidth - margin, y, bodyPaint)
    y += 20f
    canvas.drawText("Records: ${ordered.size}  |  Bills: ${billEntries.size}  |  Payments: ${paymentEntries.size}", margin, y, headerPaint)
    y += 18f
    canvas.drawText("Grand Total: ${formatAmount(grandTotal)}", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Bill Paid: ${formatAmount(grandPaid)}", margin, y, bodyPaint)
    y += 18f
    canvas.drawText("Payments Received: ${formatAmount(totalReceived)}", margin, y, bodyPaint)
    y += 22f
    canvas.drawText("Net Due: ${formatAmount(grandDue)}", margin, y, netDuePaint)

    if (y > pageHeight - margin - 24f) {
        pdf.finishPage(page)
    } else {
        pdf.finishPage(page)
    }

    val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outFile = File(outDir, "customer_ledger_$timeTag.pdf")
    FileOutputStream(outFile).use { pdf.writeTo(it) }
    pdf.close()
    return outFile
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('\n')) "\"$escaped\"" else escaped
}
