package org.lapka.sms.extensions

import org.junit.Test

class CollectionExtensionsTest {

    @Test
    fun `associateByNotNull creates map from non-null keys`() {
        val items = listOf("apple", "banana", "cherry")
        val result = items.associateByNotNull { it.first().toString() }

        assert(result.size == 3)
        assert(result["a"] == "apple")
        assert(result["b"] == "banana")
        assert(result["c"] == "cherry")
    }

    @Test
    fun `associateByNotNull skips null keys`() {
        val items = listOf(1, 2, 3, 4, 5)
        val result = items.associateByNotNull { if (it % 2 == 0) it.toString() else null }

        assert(result.size == 2)
        assert(result["2"] == 2)
        assert(result["4"] == 4)
        assert(!result.containsKey("1"))
    }

    @Test
    fun `associateByNotNull returns empty map for empty iterable`() {
        val result = emptyList<String>().associateByNotNull { it }

        assert(result.isEmpty())
    }

    @Test
    fun `associateByNotNull returns empty map when all keys are null`() {
        val items = listOf("a", "b", "c")
        val result = items.associateByNotNull<String?, String> { null }

        assert(result.isEmpty())
    }

    @Test
    fun `associateByNotNull last value wins for duplicate keys`() {
        val items = listOf("alpha", "also", "beta")
        val result = items.associateByNotNull { it.first().toString() }

        // "also" should overwrite "alpha" since both have key "a"
        assert(result["a"] == "also")
        assert(result["b"] == "beta")
        assert(result.size == 2)
    }
}
