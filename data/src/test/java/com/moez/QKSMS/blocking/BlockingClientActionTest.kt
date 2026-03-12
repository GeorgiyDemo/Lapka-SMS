package com.moez.QKSMS.blocking

import org.junit.Test

/**
 * Tests for BlockingClient.Action sealed class behavior.
 */
class BlockingClientActionTest {

    @Test
    fun `Block toString returns Block`() {
        val action = BlockingClient.Action.Block()
        assert(action.toString() == "Block")
    }

    @Test
    fun `Block with reason toString returns Block`() {
        val action = BlockingClient.Action.Block("spam")
        assert(action.toString() == "Block")
    }

    @Test
    fun `Unblock toString returns Unblock`() {
        assert(BlockingClient.Action.Unblock.toString() == "Unblock")
    }

    @Test
    fun `DoNothing toString returns DoNothing`() {
        assert(BlockingClient.Action.DoNothing.toString() == "DoNothing")
    }

    @Test
    fun `Block stores reason`() {
        val action = BlockingClient.Action.Block("blacklisted")
        assert(action.reason == "blacklisted")
    }

    @Test
    fun `Block has null reason by default`() {
        val action = BlockingClient.Action.Block()
        assert(action.reason == null)
    }

    @Test
    fun `Block instances are distinct objects`() {
        val a = BlockingClient.Action.Block()
        val b = BlockingClient.Action.Block()
        // Block is a class, not object, so instances are different
        assert(a !== b)
    }

    @Test
    fun `Unblock is singleton`() {
        val a = BlockingClient.Action.Unblock
        val b = BlockingClient.Action.Unblock
        assert(a === b)
    }

    @Test
    fun `DoNothing is singleton`() {
        val a = BlockingClient.Action.DoNothing
        val b = BlockingClient.Action.DoNothing
        assert(a === b)
    }

    @Test
    fun `type checks work correctly`() {
        val block: BlockingClient.Action = BlockingClient.Action.Block()
        val unblock: BlockingClient.Action = BlockingClient.Action.Unblock
        val doNothing: BlockingClient.Action = BlockingClient.Action.DoNothing

        assert(block is BlockingClient.Action.Block)
        assert(block !is BlockingClient.Action.Unblock)
        assert(unblock is BlockingClient.Action.Unblock)
        assert(doNothing is BlockingClient.Action.DoNothing)
    }
}
