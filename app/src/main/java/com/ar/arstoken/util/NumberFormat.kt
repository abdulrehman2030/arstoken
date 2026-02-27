package com.ar.arstoken.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

fun round2(value: Double): Double =
    BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()

fun formatAmount(value: Double): String =
    String.format(Locale.US, "%.2f", round2(value))

fun formatQty(value: Double): String {
    val rounded = round2(value)
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded)
    }
}
