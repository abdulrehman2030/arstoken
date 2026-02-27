package com.ar.arstoken.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val id: Int = 1,   // single row only
    val storeName: String,
    val phone: String,
    val primaryPrinterEnabled: Boolean = true,
    val printerType: String = "BLUETOOTH",
    val autoPrintSale: Boolean = true,
    val charactersPerLine: Int = 32,
    val column2Chars: Int = 4,
    val column3Chars: Int = 6,
    val column4Chars: Int = 6,
    val printTokenNumber: Boolean = false,
    val businessNameSize: String = "MEDIUM",
    val totalAmountSize: String = "SMALL",
    val printCreatedInfo: Boolean = true,
    val printFooter: String = "Thank You! Visit Again!",
    val printerSpacingFix: Boolean = false,
    val bottomPaddingLines: Int = 1,
    val printItemMultiLine: Boolean = true,
    val itemOrder: String = "INSERTED",
    val syncEnabled: Boolean = true,
    val syncHour: Int = 22,
    val syncMinute: Int = 0,
    val updatedAt: Long = 0
)
