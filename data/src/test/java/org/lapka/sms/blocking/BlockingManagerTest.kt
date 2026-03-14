package org.lapka.sms.blocking

import com.f2prateek.rx.preferences2.Preference
import org.lapka.sms.util.Preferences
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class BlockingManagerTest {

    @Mock
    lateinit var prefs: Preferences

    @Mock
    lateinit var callBlockerClient: CallBlockerBlockingClient

    @Mock
    lateinit var callControlClient: CallControlBlockingClient

    @Mock
    lateinit var qksmsClient: QksmsBlockingClient

    @Mock
    lateinit var shouldIAnswerClient: ShouldIAnswerBlockingClient

    @Mock
    lateinit var blockingManagerPref: Preference<Int>

    private lateinit var blockingManager: BlockingManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(prefs.blockingManager).thenReturn(blockingManagerPref)
        blockingManager = BlockingManager(prefs, callBlockerClient, callControlClient, qksmsClient, shouldIAnswerClient)
    }

    // --- Client selection tests ---

    @Test
    fun `delegates to qksms client when pref is BLOCKING_MANAGER_QKSMS`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        `when`(qksmsClient.isAvailable()).thenReturn(true)

        assert(blockingManager.isAvailable())
        verify(qksmsClient).isAvailable()
    }

    @Test
    fun `delegates to call blocker client when pref is BLOCKING_MANAGER_CB`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_CB)
        `when`(callBlockerClient.isAvailable()).thenReturn(true)

        assert(blockingManager.isAvailable())
        verify(callBlockerClient).isAvailable()
    }

    @Test
    fun `delegates to should i answer client when pref is BLOCKING_MANAGER_SIA`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_SIA)
        `when`(shouldIAnswerClient.isAvailable()).thenReturn(false)

        assert(!blockingManager.isAvailable())
        verify(shouldIAnswerClient).isAvailable()
    }

    @Test
    fun `delegates to call control client when pref is BLOCKING_MANAGER_CC`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_CC)
        `when`(callControlClient.isAvailable()).thenReturn(false)

        assert(!blockingManager.isAvailable())
        verify(callControlClient).isAvailable()
    }

    @Test
    fun `defaults to qksms client for unknown pref value`() {
        `when`(blockingManagerPref.get()).thenReturn(999)
        `when`(qksmsClient.isAvailable()).thenReturn(true)

        assert(blockingManager.isAvailable())
        verify(qksmsClient).isAvailable()
    }

    // --- Capability delegation ---

    @Test
    fun `getClientCapability delegates to active client`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        `when`(qksmsClient.getClientCapability()).thenReturn(BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION)

        assert(blockingManager.getClientCapability() == BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION)
    }

    @Test
    fun `getClientCapability returns CANT_BLOCK for SIA`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_SIA)
        `when`(shouldIAnswerClient.getClientCapability()).thenReturn(BlockingClient.Capability.CANT_BLOCK)

        assert(blockingManager.getClientCapability() == BlockingClient.Capability.CANT_BLOCK)
    }

    // --- shouldBlock delegation ---

    @Test
    fun `shouldBlock delegates to active client`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        val expected = Single.just<BlockingClient.Action>(BlockingClient.Action.Block())
        `when`(qksmsClient.shouldBlock("123")).thenReturn(expected)

        val result = blockingManager.shouldBlock("123")
        assert(result === expected)
        verify(qksmsClient).shouldBlock("123")
    }

    // --- block delegation ---

    @Test
    fun `block delegates to active client`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        val addresses = listOf("123", "456")
        val expected = Completable.complete()
        `when`(qksmsClient.block(addresses)).thenReturn(expected)

        val result = blockingManager.block(addresses)
        assert(result === expected)
        verify(qksmsClient).block(addresses)
    }

    // --- unblock delegation ---

    @Test
    fun `unblock delegates to active client`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        val addresses = listOf("789")
        val expected = Completable.complete()
        `when`(qksmsClient.unblock(addresses)).thenReturn(expected)

        val result = blockingManager.unblock(addresses)
        assert(result === expected)
        verify(qksmsClient).unblock(addresses)
    }

    // --- openSettings delegation ---

    @Test
    fun `openSettings delegates to active client`() {
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_CB)

        blockingManager.openSettings()
        verify(callBlockerClient).openSettings()
    }

    // --- Dynamic switching ---

    @Test
    fun `changing pref value switches the active client`() {
        // Start with QKSMS
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_QKSMS)
        `when`(qksmsClient.isAvailable()).thenReturn(true)
        assert(blockingManager.isAvailable())
        verify(qksmsClient).isAvailable()

        // Switch to CB
        `when`(blockingManagerPref.get()).thenReturn(Preferences.BLOCKING_MANAGER_CB)
        `when`(callBlockerClient.isAvailable()).thenReturn(false)
        assert(!blockingManager.isAvailable())
        verify(callBlockerClient).isAvailable()
    }
}
