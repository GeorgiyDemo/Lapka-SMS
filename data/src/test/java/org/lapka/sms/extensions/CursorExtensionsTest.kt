package org.lapka.sms.extensions

import android.database.Cursor
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.times

class CursorExtensionsTest {

    private fun mockCursor(rowCount: Int): Cursor {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(rowCount)

        // Track position for moveToPosition and moveToNext
        var position = -1
        `when`(cursor.moveToPosition(org.mockito.ArgumentMatchers.anyInt())).thenAnswer { invocation ->
            position = invocation.getArgument(0)
            position in 0 until rowCount
        }
        `when`(cursor.moveToNext()).thenAnswer {
            position++
            position < rowCount
        }

        return cursor
    }

    // --- forEach tests ---

    @Test
    fun `forEach iterates over all rows`() {
        val cursor = mockCursor(3)
        var count = 0

        cursor.forEach { count++ }

        assert(count == 3) { "Expected 3 iterations but got $count" }
    }

    @Test
    fun `forEach closes cursor when closeOnComplete is true`() {
        val cursor = mockCursor(2)

        cursor.forEach(closeOnComplete = true) {}

        verify(cursor).close()
    }

    @Test
    fun `forEach does not close cursor when closeOnComplete is false`() {
        val cursor = mockCursor(2)

        cursor.forEach(closeOnComplete = false) {}

        verify(cursor, never()).close()
    }

    @Test
    fun `forEach with empty cursor does not invoke method`() {
        val cursor = mockCursor(0)
        var called = false

        cursor.forEach { called = true }

        assert(!called) { "Method should not have been called for empty cursor" }
    }

    @Test
    fun `forEach resets position to -1 before iterating`() {
        val cursor = mockCursor(1)

        cursor.forEach {}

        verify(cursor).moveToPosition(-1)
    }

    @Test
    fun `forEach closes cursor by default`() {
        val cursor = mockCursor(0)

        cursor.forEach()

        verify(cursor).close()
    }

    // --- map tests ---

    @Test
    fun `map returns list of mapped values`() {
        val cursor = mockCursor(3)
        var callCount = 0
        `when`(cursor.getString(0)).thenAnswer {
            "value_$callCount"
        }

        val result = cursor.map {
            callCount++
            "item_$callCount"
        }

        assert(result.size == 3) { "Expected 3 items but got ${result.size}" }
        assert(result == listOf("item_1", "item_2", "item_3")) { "Unexpected result: $result" }
    }

    @Test
    fun `map returns empty list for empty cursor`() {
        val cursor = mockCursor(0)

        val result = cursor.map { "anything" }

        assert(result.isEmpty()) { "Expected empty list" }
    }

    @Test
    fun `map calls moveToPosition for each row`() {
        val cursor = mockCursor(3)

        cursor.map { "x" }

        verify(cursor).moveToPosition(0)
        verify(cursor).moveToPosition(1)
        verify(cursor).moveToPosition(2)
    }

    // --- dump tests ---

    @Test
    fun `dump produces CSV with header and data rows`() {
        val cursor = mockCursor(2)
        `when`(cursor.columnNames).thenReturn(arrayOf("id", "name"))
        `when`(cursor.columnCount).thenReturn(2)

        var row = 0
        `when`(cursor.getString(org.mockito.ArgumentMatchers.anyInt())).thenAnswer { invocation ->
            val col = invocation.getArgument<Int>(0)
            when {
                row == 0 && col == 0 -> "1"
                row == 0 && col == 1 -> "Alice"
                row == 1 && col == 0 -> "2"
                row == 1 && col == 1 -> "Bob"
                else -> ""
            }
        }

        // forEach increments rows via moveToNext
        var position = -1
        `when`(cursor.moveToPosition(org.mockito.ArgumentMatchers.anyInt())).thenAnswer {
            position = it.getArgument(0)
            true
        }
        `when`(cursor.moveToNext()).thenAnswer {
            position++
            row = position
            position < 2
        }

        val result = cursor.dump()
        val lines = result.split("\n")

        assert(lines.size == 3) { "Expected 3 lines (header + 2 data rows), got ${lines.size}" }
        assert(lines[0] == "id,name") { "Header mismatch: ${lines[0]}" }
    }

    @Test
    fun `dump with empty cursor returns only header`() {
        val cursor = mockCursor(0)
        `when`(cursor.columnNames).thenReturn(arrayOf("col1", "col2"))

        val result = cursor.dump()

        assert(result == "col1,col2") { "Expected only header, got: $result" }
    }
}
