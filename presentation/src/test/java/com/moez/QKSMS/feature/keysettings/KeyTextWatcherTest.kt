package com.moez.QKSMS.feature.keysettings

import android.text.Editable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class KeyTextWatcherTest {

    private lateinit var watcher: KeyTextWatcher
    private lateinit var observer: TestObserver<String>

    @Before
    fun setUp() {
        watcher = KeyTextWatcher()
        observer = watcher.keyChanged.test()
    }

    private fun mockEditable(text: String): Editable {
        val editable = mock(Editable::class.java)
        `when`(editable.toString()).thenReturn(text)
        return editable
    }

    @Test
    fun `keyChanged emits text after debounce period`() {
        watcher.afterTextChanged(mockEditable("hello"))

        // Wait for debounce (300ms + margin)
        Thread.sleep(450)

        observer.assertValueCount(1)
        observer.assertValueAt(0, "hello")
    }

    @Test
    fun `rapid typing only emits final value after debounce`() {
        watcher.afterTextChanged(mockEditable("h"))
        Thread.sleep(100)
        watcher.afterTextChanged(mockEditable("he"))
        Thread.sleep(100)
        watcher.afterTextChanged(mockEditable("hel"))
        Thread.sleep(100)
        watcher.afterTextChanged(mockEditable("hell"))
        Thread.sleep(100)
        watcher.afterTextChanged(mockEditable("hello"))

        // Wait for debounce
        Thread.sleep(450)

        observer.assertValueCount(1)
        observer.assertValueAt(0, "hello")
    }

    @Test
    fun `emits empty string when text is cleared`() {
        watcher.afterTextChanged(mockEditable(""))
        Thread.sleep(450)

        observer.assertValueCount(1)
        observer.assertValueAt(0, "")
    }

    @Test
    fun `emits multiple values when typing pauses between words`() {
        watcher.afterTextChanged(mockEditable("first"))
        Thread.sleep(450)

        watcher.afterTextChanged(mockEditable("second"))
        Thread.sleep(450)

        observer.assertValueCount(2)
        observer.assertValueAt(0, "first")
        observer.assertValueAt(1, "second")
    }

    @Test
    fun `no emission before debounce period elapses`() {
        watcher.afterTextChanged(mockEditable("test"))
        Thread.sleep(100) // Well before the 300ms debounce

        observer.assertNoValues()
    }

    @Test
    fun `beforeTextChanged and onTextChanged do not emit`() {
        watcher.beforeTextChanged("test", 0, 4, 0)
        watcher.onTextChanged("test", 0, 0, 4)

        Thread.sleep(450)
        observer.assertNoValues()
    }
}
