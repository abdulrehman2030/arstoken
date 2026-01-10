package com.ar.arstoken.util

import android.content.Context
import android.content.Intent

fun shareText(
    context: Context,
    text: String,
    title: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(
        Intent.createChooser(intent, title)
    )
}
