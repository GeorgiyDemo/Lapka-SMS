package org.lapka.sms.common.util.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberExtensionsTest {

    // --- forEach ---

    @Test
    fun `forEach invokes action for each index from 0 to n-1`() {
        val collected = mutableListOf<Int>()
        5.forEach { collected.add(it) }
        assertEquals(listOf(0, 1, 2, 3, 4), collected)
    }

    @Test
    fun `forEach with zero invokes nothing`() {
        val collected = mutableListOf<Int>()
        0.forEach { collected.add(it) }
        assertEquals(emptyList<Int>(), collected)
    }

    @Test
    fun `forEach with one invokes action once with index 0`() {
        val collected = mutableListOf<Int>()
        1.forEach { collected.add(it) }
        assertEquals(listOf(0), collected)
    }

    @Test
    fun `forEach with negative number invokes nothing`() {
        val collected = mutableListOf<Int>()
        (-3).forEach { collected.add(it) }
        assertEquals(emptyList<Int>(), collected)
    }

    // --- within ---

    @Test
    fun `within returns value when inside range`() {
        assertEquals(5.0f, 5.0f.within(0f, 10f), 0.001f)
    }

    @Test
    fun `within clamps to min when below range`() {
        assertEquals(0f, (-5f).within(0f, 10f), 0.001f)
    }

    @Test
    fun `within clamps to max when above range`() {
        assertEquals(10f, 15f.within(0f, 10f), 0.001f)
    }

    @Test
    fun `within returns min when value equals min`() {
        assertEquals(0f, 0f.within(0f, 10f), 0.001f)
    }

    @Test
    fun `within returns max when value equals max`() {
        assertEquals(10f, 10f.within(0f, 10f), 0.001f)
    }

    @Test
    fun `within handles negative range`() {
        assertEquals(-5f, (-5f).within(-10f, -1f), 0.001f)
    }

    @Test
    fun `within clamps to min of negative range`() {
        assertEquals(-10f, (-15f).within(-10f, -1f), 0.001f)
    }

    @Test
    fun `within handles zero-width range`() {
        assertEquals(5f, 5f.within(5f, 5f), 0.001f)
    }

    @Test
    fun `within clamps to single-value range`() {
        assertEquals(5f, 10f.within(5f, 5f), 0.001f)
    }
}
