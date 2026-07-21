package com.ar.arstoken.util

import com.ar.arstoken.data.db.SaleEntity
import com.ar.arstoken.data.db.SaleItemEntity
import com.ar.arstoken.data.db.StoreSettingsEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {

    @Test
    fun testFormatReceiptBasic() {
        val settings = StoreSettingsEntity(
            storeName = "Test Corner",
            phone = "1234567890",
            charactersPerLine = 32,
            printTokenNumber = false,
            printCreatedInfo = true,
            printFooter = "Have a Great Day!"
        )

        val sale = SaleEntity(
            id = 42,
            timestamp = 1711111111000L, // Specific timestamp for reproducible test
            customerId = 1,
            customerName = "John Doe",
            saleType = "CASH",
            totalAmount = 150.0,
            paidAmount = 150.0,
            dueAmount = 0.0,
            updatedAt = 1711111111000L
        )

        val items = listOf(
            SaleItemEntity(
                id = 101,
                saleId = 42,
                itemId = 10,
                itemName = "Coffee",
                quantity = 2.0,
                unitPrice = 50,
                totalPrice = 100.0,
                timestamp = 1711111111000L
            ),
            SaleItemEntity(
                id = 102,
                saleId = 42,
                itemId = 11,
                itemName = "Cookie",
                quantity = 1.0,
                unitPrice = 50,
                totalPrice = 50.0,
                timestamp = 1711111111000L
            )
        )

        val receipt = formatReceipt(
            settings = settings,
            businessNameOverride = null,
            businessPhoneOverride = null,
            sale = sale,
            items = items
        )

        // Verify key parts of the receipt string
        assertTrue(receipt.contains("TEST CORNER"))
        assertTrue(receipt.contains("Phone: 1234567890"))
        assertTrue(receipt.contains("Bill No: 42"))
        assertTrue(receipt.contains("Bill To: John Doe"))
        assertTrue(receipt.contains("Coffee"))
        assertTrue(receipt.contains("Cookie"))
        assertTrue(receipt.contains("Total Items:"))
        assertTrue(receipt.contains("Total Quantity:"))
        assertTrue(receipt.contains("Sub Total"))
        assertTrue(receipt.contains("150.00"))
        assertTrue(receipt.contains("Have a Great Day!"))
    }

    @Test
    fun testFormatReceiptWithOptions() {
        val settings = StoreSettingsEntity(
            storeName = "Original Store Name",
            phone = "1111111111",
            charactersPerLine = 40,
            printTokenNumber = true,
            printCreatedInfo = false,
            printFooter = "Goodbye!"
        )

        val sale = SaleEntity(
            id = 99,
            timestamp = 1711111111000L,
            customerId = null,
            customerName = null,
            saleType = "CREDIT",
            totalAmount = 200.0,
            paidAmount = 50.0,
            dueAmount = 150.0,
            updatedAt = 1711111111000L
        )

        val items = listOf(
            SaleItemEntity(
                id = 201,
                saleId = 99,
                itemId = 20,
                itemName = "Very Long Item Name That Exceeds Line Width",
                quantity = 1.0,
                unitPrice = 200,
                totalPrice = 200.0,
                timestamp = 1711111111000L
            )
        )

        // Using name and phone override
        val receipt = formatReceipt(
            settings = settings,
            businessNameOverride = "Override Emporium",
            businessPhoneOverride = "9999999999",
            sale = sale,
            items = items
        )

        // Verify override names and custom fields
        assertTrue(receipt.contains("OVERRIDE EMPORIUM"))
        assertTrue(receipt.contains("Phone: 9999999999"))
        assertTrue(!receipt.contains("ORIGINAL STORE NAME"))
        assertTrue(receipt.contains("Token No: 99"))
        assertTrue(receipt.contains("Bill To: Cash Sale"))
        assertTrue(!receipt.contains("Created On:")) // should be false under printCreatedInfo = false
        assertTrue(receipt.contains("Mode of Payment"))
        assertTrue(receipt.contains("credit"))
        assertTrue(receipt.contains("Received"))
        assertTrue(receipt.contains("50.00"))
    }
}
