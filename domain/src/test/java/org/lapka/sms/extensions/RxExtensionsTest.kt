package org.lapka.sms.extensions

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Test

class RxExtensionsTest {

    // -- Optional tests --

    @Test
    fun `Optional notNull returns true when value is present`() {
        val optional = Optional("hello")
        assertTrue(optional.notNull())
    }

    @Test
    fun `Optional notNull returns false when value is null`() {
        val optional = Optional<String>(null)
        assertFalse(optional.notNull())
    }

    @Test
    fun `Optional value stores the provided value`() {
        val optional = Optional(42)
        assertEquals(42, optional.value)
    }

    @Test
    fun `Optional value is null when constructed with null`() {
        val optional = Optional<Int>(null)
        assertNull(optional.value)
    }

    @Test
    fun `Optional data class equality works correctly`() {
        assertEquals(Optional("a"), Optional("a"))
        assertNotEquals(Optional("a"), Optional("b"))
        assertNotEquals(Optional("a"), Optional<String>(null))
    }

    // -- Flowable.mapNotNull tests --

    @Test
    fun `Flowable mapNotNull filters out null mapper results`() {
        val results = Flowable.just(1, 2, 3, 4, 5)
            .mapNotNull { if (it % 2 == 0) it.toString() else null }
            .toList()
            .blockingGet()

        assertEquals(listOf("2", "4"), results)
    }

    @Test
    fun `Flowable mapNotNull emits all items when none map to null`() {
        val results = Flowable.just("a", "b", "c")
            .mapNotNull { it.uppercase() }
            .toList()
            .blockingGet()

        assertEquals(listOf("A", "B", "C"), results)
    }

    @Test
    fun `Flowable mapNotNull emits nothing when all map to null`() {
        val results = Flowable.just(1, 2, 3)
            .mapNotNull<Int, String> { null }
            .toList()
            .blockingGet()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `Flowable mapNotNull handles empty source`() {
        val results = Flowable.empty<Int>()
            .mapNotNull { it.toString() }
            .toList()
            .blockingGet()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `Flowable mapNotNull preserves emission order`() {
        val results = Flowable.just(10, 20, 30)
            .mapNotNull { if (it >= 20) it * 10 else null }
            .toList()
            .blockingGet()

        assertEquals(listOf(200, 300), results)
    }

    // -- Observable.mapNotNull tests --

    @Test
    fun `Observable mapNotNull filters out null mapper results`() {
        val results = Observable.just(1, 2, 3, 4, 5)
            .mapNotNull { if (it > 3) it else null }
            .toList()
            .blockingGet()

        assertEquals(listOf(4, 5), results)
    }

    @Test
    fun `Observable mapNotNull emits all items when none map to null`() {
        val results = Observable.just("x", "y")
            .mapNotNull { "$it!" }
            .toList()
            .blockingGet()

        assertEquals(listOf("x!", "y!"), results)
    }

    @Test
    fun `Observable mapNotNull emits nothing when all map to null`() {
        val results = Observable.just(1, 2)
            .mapNotNull<Int, String> { null }
            .toList()
            .blockingGet()

        assertTrue(results.isEmpty())
    }

    // -- Observable.toFlowable tests --

    @Test
    fun `toFlowable converts Observable to Flowable with BUFFER strategy`() {
        val observable = Observable.just(1, 2, 3)
        val results = observable.toFlowable()
            .toList()
            .blockingGet()

        assertEquals(listOf(1, 2, 3), results)
    }

    @Test
    fun `toFlowable handles empty observable`() {
        val results = Observable.empty<Int>()
            .toFlowable()
            .toList()
            .blockingGet()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `toFlowable buffers items when downstream is slow`() {
        // Emit many items rapidly — BUFFER strategy should prevent loss
        val count = 1000
        val results = Observable.range(1, count)
            .toFlowable()
            .toList()
            .blockingGet()

        assertEquals(count, results.size)
        assertEquals(1, results.first())
        assertEquals(count, results.last())
    }
}
