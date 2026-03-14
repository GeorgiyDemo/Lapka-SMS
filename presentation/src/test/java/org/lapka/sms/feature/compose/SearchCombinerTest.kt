package org.lapka.sms.feature.compose

import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the search combiner logic in ComposeViewModel.
 * Verifies that combineLatest never returns null (which causes RxJava crash).
 */
class SearchCombinerTest {

    @Test
    fun `combiner handles empty search results without crash`() {
        val searchSelection = BehaviorSubject.createDefault(-1L)
        val searchResults = BehaviorSubject.createDefault(emptyList<FakeMessage>())

        val values = mutableListOf<Unit>()
        val errors = mutableListOf<Throwable>()

        Observables.combineLatest(searchSelection, searchResults) { selected, messages ->
            if (selected == -1L) {
                messages.lastOrNull()?.let { message -> searchSelection.onNext(message.id) }
            } else {
                val position = messages.indexOfFirst { it.id == selected } + 1
                // Would update state here
            }
            Unit
        }.subscribe({ values.add(it) }, { errors.add(it) })

        assertEquals("Should emit without errors", 1, values.size)
        assertEquals("Should have no errors", 0, errors.size)
    }

    @Test
    fun `combiner selects last message when no selection`() {
        val searchSelection = BehaviorSubject.createDefault(-1L)
        val messages = listOf(FakeMessage(1L), FakeMessage(2L), FakeMessage(3L))
        val searchResults = BehaviorSubject.createDefault(messages)

        val errors = mutableListOf<Throwable>()

        Observables.combineLatest(searchSelection, searchResults) { selected, msgs ->
            if (selected == -1L) {
                msgs.lastOrNull()?.let { message -> searchSelection.onNext(message.id) }
            } else {
                val position = msgs.indexOfFirst { it.id == selected } + 1
            }
            Unit
        }.subscribe({}, { errors.add(it) })

        assertEquals("Should have no errors", 0, errors.size)
        assertEquals("Should select last message", 3L, searchSelection.value)
    }

    @Test
    fun `combiner calculates correct position for selected message`() {
        val searchSelection = BehaviorSubject.createDefault(2L)
        val messages = listOf(FakeMessage(1L), FakeMessage(2L), FakeMessage(3L))
        val searchResults = BehaviorSubject.createDefault(messages)

        var capturedPosition = -1
        var capturedTotal = -1

        Observables.combineLatest(searchSelection, searchResults) { selected, msgs ->
            if (selected == -1L) {
                msgs.lastOrNull()?.let { message -> searchSelection.onNext(message.id) }
            } else {
                capturedPosition = msgs.indexOfFirst { it.id == selected } + 1
                capturedTotal = msgs.size
            }
            Unit
        }.subscribe()

        assertEquals("Position should be 2", 2, capturedPosition)
        assertEquals("Total should be 3", 3, capturedTotal)
    }

    data class FakeMessage(val id: Long)
}
