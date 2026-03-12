package com.moez.QKSMS.blocking

import com.moez.QKSMS.repository.BlockingRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class QksmsBlockingClientTest {

    @Mock
    lateinit var blockingRepo: BlockingRepository

    private lateinit var client: QksmsBlockingClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        client = QksmsBlockingClient(blockingRepo)
    }

    // --- isAvailable ---

    @Test
    fun `isAvailable always returns true`() {
        assert(client.isAvailable())
    }

    // --- getClientCapability ---

    @Test
    fun `getClientCapability returns BLOCK_WITHOUT_PERMISSION`() {
        assert(client.getClientCapability() == BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION)
    }

    // --- shouldBlock ---

    @Test
    fun `shouldBlock returns Block when address is blocked`() {
        `when`(blockingRepo.isBlocked("555-1234")).thenReturn(true)

        val action = client.shouldBlock("555-1234").blockingGet()
        assert(action is BlockingClient.Action.Block)
    }

    @Test
    fun `shouldBlock returns Unblock when address is not blocked`() {
        `when`(blockingRepo.isBlocked("555-1234")).thenReturn(false)

        val action = client.shouldBlock("555-1234").blockingGet()
        assert(action is BlockingClient.Action.Unblock)
    }

    // --- isBlacklisted ---

    @Test
    fun `isBlacklisted returns Block when address is blocked`() {
        `when`(blockingRepo.isBlocked("555-9999")).thenReturn(true)

        val action = client.isBlacklisted("555-9999").blockingGet()
        assert(action is BlockingClient.Action.Block)
    }

    @Test
    fun `isBlacklisted returns Unblock when address is not blocked`() {
        `when`(blockingRepo.isBlocked("555-9999")).thenReturn(false)

        val action = client.isBlacklisted("555-9999").blockingGet()
        assert(action is BlockingClient.Action.Unblock)
    }

    @Test
    fun `shouldBlock delegates to isBlacklisted with same result`() {
        `when`(blockingRepo.isBlocked("111")).thenReturn(true)

        val shouldBlockAction = client.shouldBlock("111").blockingGet()
        val isBlacklistedAction = client.isBlacklisted("111").blockingGet()

        assert(shouldBlockAction is BlockingClient.Action.Block)
        assert(isBlacklistedAction is BlockingClient.Action.Block)
    }

    // --- block ---

    @Test
    fun `block calls blockNumber on repository`() {
        val addresses = listOf("111", "222", "333")

        client.block(addresses).blockingAwait()
        verify(blockingRepo).blockNumber("111", "222", "333")
    }

    @Test
    fun `block with single address calls blockNumber`() {
        client.block(listOf("555")).blockingAwait()
        verify(blockingRepo).blockNumber("555")
    }

    @Test
    fun `block with empty list calls blockNumber with no args`() {
        client.block(emptyList()).blockingAwait()
        verify(blockingRepo).blockNumber()
    }

    // --- unblock ---

    @Test
    fun `unblock calls unblockNumbers on repository`() {
        val addresses = listOf("111", "222")

        client.unblock(addresses).blockingAwait()
        verify(blockingRepo).unblockNumbers("111", "222")
    }

    @Test
    fun `unblock with single address calls unblockNumbers`() {
        client.unblock(listOf("999")).blockingAwait()
        verify(blockingRepo).unblockNumbers("999")
    }

    // --- openSettings ---

    @Test
    fun `openSettings does not throw`() {
        // openSettings() returns Unit and is a no-op
        client.openSettings()
    }

    // --- Block action reason ---

    @Test
    fun `Block action has null reason by default`() {
        `when`(blockingRepo.isBlocked("123")).thenReturn(true)

        val action = client.shouldBlock("123").blockingGet()
        assert(action is BlockingClient.Action.Block)
        assert((action as BlockingClient.Action.Block).reason == null)
    }
}
