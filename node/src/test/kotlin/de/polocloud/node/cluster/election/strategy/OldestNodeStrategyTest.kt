package de.polocloud.node.cluster.election.strategy

import de.polocloud.node.cluster.node.NodeData
import de.polocloud.proto.NodeState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock.System.now

class OldestNodeStrategyTest {

    private fun node(
        index: Int,
        state: NodeState = NodeState.ONLINE,
        head: Boolean = false,
        firstConnection: kotlin.time.Instant = now(),
        lastConnection: kotlin.time.Instant = now(),
    ) = NodeData(
        id = UUID.randomUUID(),
        index = index,
        hostname = "127.0.0.1",
        port = 4240 + index,
        state = state,
        head = head,
        version = "3.0.0",
        gitCommitHash = "abc123",
        firstConnection = firstConnection,
        lastConnection = lastConnection,
    )

    @Test
    fun `picks the candidate that joined the cluster earliest`() {
        val now = now()
        // Node 2 joined first but has been quiet the longest; node 1 joined later but was
        // just heard from. Seniority (firstConnection) must win, not recency (lastConnection).
        val senior = node(index = 2, firstConnection = now - 60.minutes, lastConnection = now - 50.minutes)
        val junior = node(index = 1, firstConnection = now - 5.minutes, lastConnection = now)

        assertEquals(senior, OldestNodeStrategy.elect(listOf(junior, senior)))
    }

    @Test
    fun `excludes offline and already-head candidates`() {
        val offline = node(index = 1, state = NodeState.OFFLINE, firstConnection = now() - 60.minutes)
        val alreadyHead = node(index = 2, head = true, firstConnection = now() - 50.minutes)
        val eligible = node(index = 3, firstConnection = now() - 10.minutes)

        assertEquals(eligible, OldestNodeStrategy.elect(listOf(offline, alreadyHead, eligible)))
    }

    @Test
    fun `returns null when there are no eligible candidates`() {
        assertNull(OldestNodeStrategy.elect(emptyList()))
        assertNull(OldestNodeStrategy.elect(listOf(node(index = 1, state = NodeState.OFFLINE))))
    }
}
