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

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('\n')) "\"$escaped\"" else escaped
}
