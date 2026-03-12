package com.moez.QKSMS.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MenuItemTest {

    @Test
    fun `MenuItem stores title and actionId`() {
        val item = MenuItem("Send", 1)
        assertEquals("Send", item.title)
        assertEquals(1, item.actionId)
    }

    @Test
    fun `MenuItem equality is based on title and actionId`() {
        val a = MenuItem("Send", 1)
        val b = MenuItem("Send", 1)
        assertEquals(a, b)
    }

    @Test
    fun `MenuItems with different titles are not equal`() {
        val a = MenuItem("Send", 1)
        val b = MenuItem("Delete", 1)
        assertNotEquals(a, b)
    }

    @Test
    fun `MenuItems with different actionIds are not equal`() {
        val a = MenuItem("Send", 1)
        val b = MenuItem("Send", 2)
        assertNotEquals(a, b)
    }

    @Test
    fun `MenuItem hashCode is consistent with equals`() {
        val a = MenuItem("Send", 1)
        val b = MenuItem("Send", 1)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `MenuItem copy works correctly`() {
        val original = MenuItem("Send", 1)
        val copy = original.copy(title = "Forward")
        assertEquals("Forward", copy.title)
        assertEquals(1, copy.actionId)
    }

    @Test
    fun `MenuItem destructuring works`() {
        val item = MenuItem("Archive", 5)
        val (title, actionId) = item
        assertEquals("Archive", title)
        assertEquals(5, actionId)
    }

    @Test
    fun `MenuItem toString contains field values`() {
        val item = MenuItem("Block", 3)
        val str = item.toString()
        assert(str.contains("Block")) { "toString should contain title" }
        assert(str.contains("3")) { "toString should contain actionId" }
    }
}
