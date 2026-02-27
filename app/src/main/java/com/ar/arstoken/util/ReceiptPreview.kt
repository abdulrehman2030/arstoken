package com.ar.arstoken.util

import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import kotlin.math.max

fun formatReceiptPreview(
    settings: StoreSettingsEntity,
    businessNameOverride: String?,
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    headerNote: String? = null
): String {
    val raw = formatReceipt(
        settings = settings,
        businessNameOverride = businessNameOverride,
        sale = sale,
        items = items,
        headerNote = headerNote
    )
    val width = settings.charactersPerLine.coerceIn(24, 64)
    return raw.lineSequence().joinToString("\n") { line ->
        renderLine(line, width)
    }
}

private fun renderLine(line: String, width: Int): String {
    var text = line
    var alignCenter = false

    if (text.startsWith("{C}")) {
        alignCenter = true
        text = text.removePrefix("{C}")
    } else if (text.startsWith("{L}")) {
        text = text.removePrefix("{L}")
    }

    text = text
        .replace("{B}", "")
        .replace("{/B}", "")
        .replace("{W2}", "")
        .trimEnd()

    return if (alignCenter) {
        val pad = max(0, (width - text.length) / 2)
        " ".repeat(pad) + text
    } else {
        text
    }
}
