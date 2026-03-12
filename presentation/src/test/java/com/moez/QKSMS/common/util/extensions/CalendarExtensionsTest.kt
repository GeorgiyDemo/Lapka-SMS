package com.moez.QKSMS.common.util.extensions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class CalendarExtensionsTest {

    private fun calendarOf(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // --- isSameDay ---

    @Test
    fun `isSameDay returns true for same date different times`() {
        val a = calendarOf(2025, Calendar.MARCH, 10, 8, 30)
        val b = calendarOf(2025, Calendar.MARCH, 10, 22, 15)
        assertTrue(a.isSameDay(b))
    }

    @Test
    fun `isSameDay returns true for identical calendars`() {
        val a = calendarOf(2025, Calendar.JANUARY, 1)
        val b = calendarOf(2025, Calendar.JANUARY, 1)
        assertTrue(a.isSameDay(b))
    }

    @Test
    fun `isSameDay returns false for consecutive days`() {
        val a = calendarOf(2025, Calendar.MARCH, 10)
        val b = calendarOf(2025, Calendar.MARCH, 11)
        assertFalse(a.isSameDay(b))
    }

    @Test
    fun `isSameDay returns false for same day different years`() {
        val a = calendarOf(2024, Calendar.MARCH, 10)
        val b = calendarOf(2025, Calendar.MARCH, 10)
        assertFalse(a.isSameDay(b))
    }

    @Test
    fun `isSameDay returns false for same day different months`() {
        val a = calendarOf(2025, Calendar.MARCH, 10)
        val b = calendarOf(2025, Calendar.APRIL, 10)
        assertFalse(a.isSameDay(b))
    }

    // --- isSameWeek ---

    @Test
    fun `isSameWeek returns true for dates in same week`() {
        // Monday and Friday of the same week
        val monday = calendarOf(2025, Calendar.MARCH, 10)
        val friday = calendarOf(2025, Calendar.MARCH, 14)
        assertTrue(monday.isSameWeek(friday))
    }

    @Test
    fun `isSameWeek returns false for dates in different weeks`() {
        val a = calendarOf(2025, Calendar.MARCH, 10) // week 11
        val b = calendarOf(2025, Calendar.MARCH, 17) // week 12
        assertFalse(a.isSameWeek(b))
    }

    @Test
    fun `isSameWeek returns false for same week number different years`() {
        val a = calendarOf(2024, Calendar.MARCH, 11)
        val b = calendarOf(2025, Calendar.MARCH, 10)
        assertFalse(a.isSameWeek(b))
    }

    // --- isSameYear ---

    @Test
    fun `isSameYear returns true for dates in same year`() {
        val a = calendarOf(2025, Calendar.JANUARY, 1)
        val b = calendarOf(2025, Calendar.DECEMBER, 31)
        assertTrue(a.isSameYear(b))
    }

    @Test
    fun `isSameYear returns false for dates in different years`() {
        val a = calendarOf(2024, Calendar.DECEMBER, 31)
        val b = calendarOf(2025, Calendar.JANUARY, 1)
        assertFalse(a.isSameYear(b))
    }
}
