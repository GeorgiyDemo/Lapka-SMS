package org.lapka.sms.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Preferences companion object constants and static methods.
 * These tests verify constant values that other parts of the codebase rely on,
 * guarding against accidental changes.
 */
class PreferencesConstantsTest {

    // -- Night mode constants --

    @Test
    fun `night mode constants have expected sequential values`() {
        assertEquals(0, Preferences.NIGHT_MODE_SYSTEM)
        assertEquals(1, Preferences.NIGHT_MODE_OFF)
        assertEquals(2, Preferences.NIGHT_MODE_ON)
        assertEquals(3, Preferences.NIGHT_MODE_AUTO)
    }

    @Test
    fun `night mode constants are all distinct`() {
        val values = setOf(
            Preferences.NIGHT_MODE_SYSTEM,
            Preferences.NIGHT_MODE_OFF,
            Preferences.NIGHT_MODE_ON,
            Preferences.NIGHT_MODE_AUTO
        )
        assertEquals(4, values.size)
    }

    // -- Text size constants --

    @Test
    fun `text size constants are ordered small to larger`() {
        assertTrue(Preferences.TEXT_SIZE_SMALL < Preferences.TEXT_SIZE_NORMAL)
        assertTrue(Preferences.TEXT_SIZE_NORMAL < Preferences.TEXT_SIZE_LARGE)
        assertTrue(Preferences.TEXT_SIZE_LARGE < Preferences.TEXT_SIZE_LARGER)
    }

    @Test
    fun `text size constants have expected values`() {
        assertEquals(0, Preferences.TEXT_SIZE_SMALL)
        assertEquals(1, Preferences.TEXT_SIZE_NORMAL)
        assertEquals(2, Preferences.TEXT_SIZE_LARGE)
        assertEquals(3, Preferences.TEXT_SIZE_LARGER)
    }

    // -- Notification preview constants --

    @Test
    fun `notification preview constants have expected values`() {
        assertEquals(0, Preferences.NOTIFICATION_PREVIEWS_ALL)
        assertEquals(1, Preferences.NOTIFICATION_PREVIEWS_NAME)
        assertEquals(2, Preferences.NOTIFICATION_PREVIEWS_NONE)
    }

    // -- Notification action constants --

    @Test
    fun `notification action constants are all distinct`() {
        val values = setOf(
            Preferences.NOTIFICATION_ACTION_NONE,
            Preferences.NOTIFICATION_ACTION_ARCHIVE,
            Preferences.NOTIFICATION_ACTION_DELETE,
            Preferences.NOTIFICATION_ACTION_BLOCK,
            Preferences.NOTIFICATION_ACTION_CALL,
            Preferences.NOTIFICATION_ACTION_READ,
            Preferences.NOTIFICATION_ACTION_REPLY
        )
        assertEquals(7, values.size)
    }

    @Test
    fun `notification action NONE is zero`() {
        assertEquals(0, Preferences.NOTIFICATION_ACTION_NONE)
    }

    // -- Send delay constants --

    @Test
    fun `send delay constants are ordered none to long`() {
        assertTrue(Preferences.SEND_DELAY_NONE < Preferences.SEND_DELAY_SHORT)
        assertTrue(Preferences.SEND_DELAY_SHORT < Preferences.SEND_DELAY_MEDIUM)
        assertTrue(Preferences.SEND_DELAY_MEDIUM < Preferences.SEND_DELAY_LONG)
    }

    // -- Swipe action constants --

    @Test
    fun `swipe action constants are all distinct`() {
        val values = setOf(
            Preferences.SWIPE_ACTION_NONE,
            Preferences.SWIPE_ACTION_ARCHIVE,
            Preferences.SWIPE_ACTION_DELETE,
            Preferences.SWIPE_ACTION_BLOCK,
            Preferences.SWIPE_ACTION_CALL,
            Preferences.SWIPE_ACTION_READ,
            Preferences.SWIPE_ACTION_UNREAD
        )
        assertEquals(7, values.size)
    }

    // -- Blocking manager constants --

    @Test
    fun `blocking manager constants are all distinct`() {
        val values = setOf(
            Preferences.BLOCKING_MANAGER_QKSMS,
            Preferences.BLOCKING_MANAGER_CC,
            Preferences.BLOCKING_MANAGER_SIA,
            Preferences.BLOCKING_MANAGER_CB
        )
        assertEquals(4, values.size)
    }

    @Test
    fun `blocking manager QKSMS is the default (zero)`() {
        assertEquals(0, Preferences.BLOCKING_MANAGER_QKSMS)
    }

    // -- Theme default dynamic --

    @Test
    fun `theme default dynamic sentinel is zero`() {
        assertEquals(0, Preferences.THEME_DEFAULT_DYNAMIC)
    }

    // -- getDefaultSchemeByLocale --

    @Test
    fun `getDefaultSchemeByLocale returns BASE64 scheme (0)`() {
        assertEquals(0, Preferences.getDefaultSchemeByLocale())
    }

    @Test
    fun `getDefaultSchemeByLocale is deterministic`() {
        val first = Preferences.getDefaultSchemeByLocale()
        val second = Preferences.getDefaultSchemeByLocale()
        assertEquals(first, second)
    }
}
