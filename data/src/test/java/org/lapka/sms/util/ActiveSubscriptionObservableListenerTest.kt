package org.lapka.sms.util

import org.lapka.sms.compat.SubscriptionInfoCompat
import org.lapka.sms.compat.SubscriptionManagerCompat
import io.reactivex.Observer
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class ActiveSubscriptionObservableListenerTest {

    @Mock
    lateinit var subscriptionManager: SubscriptionManagerCompat

    private lateinit var observer: Observer<List<SubscriptionInfoCompat>>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        @Suppress("UNCHECKED_CAST")
        observer = mock(Observer::class.java) as Observer<List<SubscriptionInfoCompat>>
    }

    @Test
    fun `listener is not disposed initially`() {
        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, observer)
        assert(!listener.isDisposed)
    }

    @Test
    fun `dispose marks listener as disposed`() {
        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, observer)
        listener.dispose()
        assert(listener.isDisposed)
    }

    @Test
    fun `dispose calls removeOnSubscriptionsChangedListener`() {
        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, observer)
        listener.dispose()
        verify(subscriptionManager).removeOnSubscriptionsChangedListener(listener)
    }

    @Test
    fun `onSubscriptionsChanged emits active list when not disposed`() {
        val expectedList = emptyList<SubscriptionInfoCompat>()
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(expectedList)

        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, observer)
        listener.onSubscriptionsChanged()

        verify(observer).onNext(expectedList)
    }

    @Test
    fun `onSubscriptionsChanged does not emit when disposed`() {
        val emittedValues = mutableListOf<List<SubscriptionInfoCompat>>()
        val trackingObserver = object : Observer<List<SubscriptionInfoCompat>> {
            override fun onSubscribe(d: io.reactivex.disposables.Disposable) {}
            override fun onNext(t: List<SubscriptionInfoCompat>) {
                emittedValues.add(t)
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        }

        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, trackingObserver)
        listener.dispose()
        listener.onSubscriptionsChanged()

        assert(emittedValues.isEmpty()) { "Expected no emissions after dispose, got ${emittedValues.size}" }
    }

    @Test
    fun `multiple disposes are idempotent`() {
        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, observer)
        listener.dispose()
        listener.dispose()
        assert(listener.isDisposed)
    }

    @Test
    fun `listener emits on each subscription change`() {
        val emissions = mutableListOf<List<SubscriptionInfoCompat>>()
        val trackingObserver = object : Observer<List<SubscriptionInfoCompat>> {
            override fun onSubscribe(d: io.reactivex.disposables.Disposable) {}
            override fun onNext(t: List<SubscriptionInfoCompat>) {
                emissions.add(t)
            }

            override fun onError(e: Throwable) {}
            override fun onComplete() {}
        }

        val list1 = emptyList<SubscriptionInfoCompat>()
        val list2 = emptyList<SubscriptionInfoCompat>()
        `when`(subscriptionManager.activeSubscriptionInfoList).thenReturn(list1, list2)

        val listener = ActiveSubscriptionObservable.Listener(subscriptionManager, trackingObserver)
        listener.onSubscriptionsChanged()
        listener.onSubscriptionsChanged()

        assert(emissions.size == 2) { "Expected 2 emissions, got ${emissions.size}" }
    }
}
