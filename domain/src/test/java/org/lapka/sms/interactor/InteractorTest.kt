package org.lapka.sms.interactor

import io.reactivex.Flowable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InteractorTest {

    /**
     * Concrete test implementation of the abstract Interactor class.
     * Tracks whether buildObservable was called and with what params.
     */
    private class TestInteractor : Interactor<String>() {
        var lastParams: String? = null
        var buildObservableCallCount = 0
        var itemsToEmit: List<String> = listOf("result")
        var errorToThrow: Throwable? = null

        override fun buildObservable(params: String): Flowable<*> {
            lastParams = params
            buildObservableCallCount++
            return if (errorToThrow != null) {
                Flowable.error<String>(errorToThrow!!)
            } else {
                Flowable.fromIterable(itemsToEmit)
            }
        }
    }

    private class UnitInteractor : Interactor<Unit>() {
        var called = false
        override fun buildObservable(params: Unit): Flowable<*> {
            called = true
            return Flowable.just(Unit)
        }
    }

    @Before
    fun setUp() {
        // Override schedulers so that subscribeOn(io) and observeOn(mainThread) both run on trampoline
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `execute passes params to buildObservable`() {
        val interactor = TestInteractor()
        interactor.execute("hello")
        assertEquals("hello", interactor.lastParams)
    }

    @Test
    fun `execute calls buildObservable exactly once per call`() {
        val interactor = TestInteractor()
        assertEquals(0, interactor.buildObservableCallCount)

        interactor.execute("a")
        assertEquals(1, interactor.buildObservableCallCount)

        interactor.execute("b")
        assertEquals(2, interactor.buildObservableCallCount)
    }

    @Test
    fun `execute invokes onComplete callback after stream completes`() {
        val interactor = TestInteractor()
        var completed = false
        interactor.execute("test") { completed = true }
        assertTrue("onComplete should have been called", completed)
    }

    @Test
    fun `execute does not invoke onComplete when stream errors`() {
        val interactor = TestInteractor()
        interactor.errorToThrow = RuntimeException("boom")
        var completed = false
        interactor.execute("test") { completed = true }
        assertFalse("onComplete should not be called on error", completed)
    }

    @Test
    fun `execute with default onComplete does not crash`() {
        val interactor = TestInteractor()
        // Should not throw — default onComplete is a no-op
        interactor.execute("test")
    }

    @Test
    fun `execute handles error in stream without crashing`() {
        val interactor = TestInteractor()
        interactor.errorToThrow = IllegalStateException("test error")
        // Should not throw — errors are swallowed by Timber::w subscriber
        interactor.execute("test")
    }

    @Test
    fun `isDisposed returns false initially`() {
        val interactor = TestInteractor()
        assertFalse(interactor.isDisposed)
    }

    @Test
    fun `dispose marks interactor as disposed`() {
        val interactor = TestInteractor()
        interactor.dispose()
        assertTrue(interactor.isDisposed)
    }

    @Test
    fun `dispose then execute still works for new subscriptions`() {
        // CompositeDisposable once disposed will not accept new disposables,
        // so execute after dispose should not call buildObservable effectively
        // (the subscription is immediately disposed). But it should not crash.
        val interactor = TestInteractor()
        interactor.dispose()
        // This should not throw
        interactor.execute("after-dispose")
        // buildObservable is still called, but the subscription is immediately disposed
        assertEquals("after-dispose", interactor.lastParams)
    }

    @Test
    fun `interactor works with Unit params`() {
        val interactor = UnitInteractor()
        interactor.execute(Unit)
        assertTrue(interactor.called)
    }

    @Test
    fun `multiple executions accumulate disposables until dispose`() {
        val interactor = TestInteractor()
        interactor.execute("1")
        interactor.execute("2")
        interactor.execute("3")
        assertFalse(interactor.isDisposed)
        interactor.dispose()
        assertTrue(interactor.isDisposed)
    }
}
