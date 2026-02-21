package dev.httpmarco.polocloud.node.test

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.common.LOCAL_ADDRESS
import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.files.deleteComplete
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.awaitility.Awaitility.await
import java.util.UUID
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.test.BeforeTest
import org.junit.jupiter.api.*
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.TimeUnit

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClusterNodeAuthTest {

    private val nodeCount = Random.nextInt(1, 10)
    private val clusterInstances = arrayListOf<NodeInstance>()

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true)
        }
    }

    @BeforeTest
    fun setupCluster() {
        postgres.start()
        System.out.printf("Waiting for node initialization...: $nodeCount")

        repeat(nodeCount) { i ->
            val config = NodeLaunchConfig(
                rootDir = Path("testing-${UUID.randomUUID()}"),
                address = LOCAL_ADDRESS.withPort(5600 + i),
                database = DatabaseCredentials.PostgreSQL(Address(postgres.host, postgres.firstMappedPort), "test", "test", "testdb")
            )
            val node = NodeInstance(config)
            node.shutdownHandler.running = true
            clusterInstances.add(node)
            node.start()
        }
    }

    @Test
    fun check() {
        clusterInstances.forEach {
            assert(it.cluster.state() == NodeState.ONLINE)
        }
    }

    @AfterEach
    fun teardown() {
        clusterInstances.forEach {
            try {
                it.close(ShutdownMode.GRACEFUL)

                await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .untilAsserted {
                        val state = it.cluster.state()
                        Assertions.assertTrue(
                            state == NodeState.STOPPED || state == NodeState.OFFLINE,
                            "Node state must be STOPPED or OFFLINE but was $state"
                        )
                    }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                it.launchConfig.rootDir.deleteComplete()
            }
        }
        clusterInstances.clear()
    }
}