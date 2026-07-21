package com.ar.arstoken.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateUtilsAndNumberFormatTest {

    @Test
    fun testRound2() {
        assertEquals(2.35, round2(2.345), 0.001)
        assertEquals(2.34, round2(2.344), 0.001)
        assertEquals(2.0, round2(2.0), 0.001)
        assertEquals(0.0, round2(0.0), 0.001)
        assertEquals(-2.35, round2(-2.345), 0.001)
    }

    @Test
    fun testFormatAmount() {
        assertEquals("12.35", formatAmount(12.345))
        assertEquals("12.34", formatAmount(12.344))
        assertEquals("12.00", formatAmount(12.0))
        assertEquals("0.00", formatAmount(0.0))
    }

    @Test
    fun testFormatQty() {
        assertEquals("12", formatQty(12.0))
        assertEquals("12.50", formatQty(12.5))
        assertEquals("12.34", formatQty(12.344))
        assertEquals("0", formatQty(0.0))
    }

    @Test
    fun testDateUtilsRanges() {
        val todayStart = startOfToday()
        val todayEnd = endOfToday()

        assertTrue("End of today must be after start of today", todayEnd > todayStart)
        // Difference should be exactly 23 hours, 59 minutes, 59 seconds, and 999 milliseconds
        val oneDayInMsMinusOne = 24 * 60 * 60 * 1000L - 1
        assertEquals("Difference between end and start of today should be 1 day minus 1 ms", oneDayInMsMinusOne, todayEnd - todayStart)

        val monthStart = startOfMonth()
        assertTrue("Start of month must be less than or equal to start of today", monthStart <= todayStart)

        val weekStart = startOfWeek()
        // Start of week can be in the past, or equal to today
        assertTrue("Start of week must be less than or equal to start of today + 6 days", weekStart <= todayStart + 6 * 24 * 3600 * 1000L)
    }
}
