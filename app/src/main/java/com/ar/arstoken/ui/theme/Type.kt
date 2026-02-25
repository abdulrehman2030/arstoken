package com.ar.arstoken.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlin.math.max

private fun shrink(textStyle: TextStyle): TextStyle {
    val fontSize = if (textStyle.fontSize.isSpecified && textStyle.fontSize.type == TextUnitType.Sp) {
        max(1f, textStyle.fontSize.value - 2f).sp
    } else {
        TextUnit.Unspecified
    }
    val lineHeight = if (textStyle.lineHeight.isSpecified && textStyle.lineHeight.type == TextUnitType.Sp) {
        max(1f, textStyle.lineHeight.value - 2f).sp
    } else {
        TextUnit.Unspecified
    }
    return textStyle.copy(
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}

private val BaseTypography = Typography()

val Typography = Typography(
    displayLarge = shrink(BaseTypography.displayLarge),
    displayMedium = shrink(BaseTypography.displayMedium),
    displaySmall = shrink(BaseTypography.displaySmall),
    headlineLarge = shrink(BaseTypography.headlineLarge),
    headlineMedium = shrink(BaseTypography.headlineMedium),
    headlineSmall = shrink(BaseTypography.headlineSmall),
    titleLarge = shrink(BaseTypography.titleLarge),
    titleMedium = shrink(BaseTypography.titleMedium),
    titleSmall = shrink(BaseTypography.titleSmall),
    bodyLarge = shrink(BaseTypography.bodyLarge),
    bodyMedium = shrink(BaseTypography.bodyMedium),
    bodySmall = shrink(BaseTypography.bodySmall),
    labelLarge = shrink(BaseTypography.labelLarge),
    labelMedium = shrink(BaseTypography.labelMedium),
    labelSmall = shrink(BaseTypography.labelSmall)
)
