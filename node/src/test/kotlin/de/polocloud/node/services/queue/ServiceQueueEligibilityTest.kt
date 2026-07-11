package de.polocloud.node.services.queue

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseCredentials
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.group.Group
import de.polocloud.node.group.TemplateCodec
import de.polocloud.node.services.ServiceProvider
import de.polocloud.node.services.cluster.PeerServiceQuery
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.proto.NodeState
import de.polocloud.proto.ProtoServiceProcessData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Covers [ServiceQueue]'s node-eligibility and cluster-wide round-robin `minOnline`
 * math. [ServiceQueue.groups] and [ServiceQueue.onlineNodes] are injected directly
 * (mirroring how `ListServicesServerHandler`/`FindServicesServerHandler` inject their
 * `peers` supplier for the same reason), but queuing a service still persists the
 * placeholder via [ServiceProvider.update] like it does in production, so this needs a
 * real (throwaway, file-backed) H2 database rather than a fake repository.
 */
class ServiceQueueEligibilityTest {

    companion object {
        // Relative: DatabaseCredentials.H2 always builds its JDBC URL as "jdbc:h2:file:./<path>",
        // so an absolute (e.g. Windows drive-letter) path here would produce an invalid URL.
        private val dbPath = "build/tmp/polocloud-service-queue-test-${UUID.randomUUID()}"

        @JvmStatic
        @BeforeAll
        fun setUpDatabase() {
            // The connection/close paths log through the i18n helpers, which require the
            // TranslationService to be initialised once before any database access.
            runCatching { TranslationService.init() }
            DatabaseAccess.initialize(DatabaseCredentials.H2(dbPath))
            check(DatabaseAccess.connect()) { "Failed to connect to the test H2 database" }
        }

        @JvmStatic
        @AfterAll
        fun tearDownDatabase() {
            DatabaseAccess.close()
            File(dbPath).parentFile?.listFiles { file -> file.name.startsWith(File(dbPath).name) }
                ?.forEach { it.delete() }
        }
    }

    // Lexicographically ordered so sorting by id.toString() is predictable in tests.
    private val selfId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val peerAId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val peerBId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    private fun node(id: UUID, name: String) =
        NodeData(id = id, index = 1, groupName = name, hostname = "10.0.0.1", port = 4240, state = NodeState.ONLINE, version = "3", gitCommitHash = "abc")

    private fun group(name: String = "lobby", minOnline: Long = 1, nodes: List<String> = emptyList()) =
        Group(name, 512, 0.0, minOnline, 10, "PAPER", "1.21", nodesJson = TemplateCodec.encode(nodes))

    private fun queue(
        provider: ServiceProvider = ServiceProvider(nodeId = selfId.toString()),
        online: List<NodeData>,
        groups: List<Group>,
        peerQuery: PeerServiceQuery = PeerServiceQuery { _, _ -> emptyList() },
    ) = ServiceQueue(
        factory = FactoryService(PlatformService(), provider),
        serviceProvider = provider,
        groups = { groups },
        onlineNodes = { online },
        peerQuery = peerQuery,
    )

    private fun peerService(groupName: String, index: Int) = ProtoServiceProcessData.newBuilder()
        .setUuid(UUID.randomUUID().toString()).setPlan(groupName).setIndex(index).setState("RUNNING").build()

    @Test
    fun `unrestricted group with nothing running assigns exactly one replica to the owning node`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val peerB = node(peerBId, "node-c")
        val g = group(minOnline = 3)

        val q = queue(online = listOf(self, peerA, peerB), groups = listOf(g))
        q.enqueueRequiredForTest()

        // self sorts first (selfId < peerAId < peerBId) -> position 0 owns replica k=0.
        assertEquals(listOf(1), q.queuedIndexes("lobby"))
    }

    @Test
    fun `a node whose share is already covered by cluster running count enqueues nothing`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val peerB = node(peerBId, "node-c")
        val g = group(minOnline = 3)

        // peerA already runs one instance; self (position 0) and peerB (position 2) don't
        // own any of the round-robin slots that follow from clusterRunning=1.
        val q = queue(
            online = listOf(self, peerA, peerB),
            groups = listOf(g),
            peerQuery = PeerServiceQuery { peer, _ -> if (peer.id == peerAId) listOf(peerService("lobby", 1)) else emptyList() },
        )
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a node excluded from the group's node whitelist never enqueues`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val g = group(minOnline = 5, nodes = listOf(peerA.name()))

        val q = queue(online = listOf(self, peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `a group whitelisted to just this node claims its whole minOnline locally`() {
        val self = node(selfId, "node-a")
        val peerA = node(peerAId, "node-b")
        val g = group(minOnline = 2, nodes = listOf(self.name()))

        val q = queue(online = listOf(self, peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertEquals(listOf(1, 2), q.queuedIndexes("lobby").sorted())
    }

    @Test
    fun `no eligible online node leaves the group untouched`() {
        val peerA = node(peerAId, "node-b")
        // self is not online, and the group is not restricted to peerA either - but peerA
        // is the only online node and self isn't part of the eligible set at all.
        val g = group(minOnline = 3, nodes = listOf("node-does-not-exist"))

        val q = queue(online = listOf(peerA), groups = listOf(g))
        q.enqueueRequiredForTest()

        assertTrue(q.queuedIndexes("lobby").isEmpty())
    }

    @Test
    fun `next index avoids an index already used by a peer`() {
        // peerAId sorts before selfId here so self ends up at position 1, owning k=0.
        val lowId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val self = node(selfId, "node-a")
        val peer = node(lowId, "node-z")
        val g = group(minOnline = 2)

        val q = queue(
            online = listOf(self, peer),
            groups = listOf(g),
            peerQuery = PeerServiceQuery { _, _ -> listOf(peerService("lobby", 1)) },
        )
        q.enqueueRequiredForTest()

        assertEquals(listOf(2), q.queuedIndexes("lobby"))
    }
}
