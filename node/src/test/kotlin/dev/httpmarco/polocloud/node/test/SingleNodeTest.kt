package dev.httpmarco.polocloud.node.test

import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.files.deleteComplete
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

/**
 * Tests the basic lifecycle and safety behavior of a single NodeInstance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleNodeTest {

    private lateinit var instance: NodeInstance
    private lateinit var launchConfig: NodeLaunchConfig

    @BeforeEach
    fun setup() {
        // Create unique test directory for isolation
        launchConfig = NodeLaunchConfig(Path("testing-${UUID.randomUUID()}"))
        instance = NodeInstance(launchConfig)
        instance.shutdownHandler.running = true
    }

    @Test
    fun `starting node twice should throw exception`() {
        startNodeAndWaitOnline()

        assertThrows<IllegalStateException>(
            "Starting a node twice should throw IllegalStateException"
        ) {
            instance.start()
        }
    }

    @Test
    fun `closing non-started node should not crash`() {
        assertDoesNotThrow(
            "Closing a node that was never started should not throw"
        ) {
            instance.close(ShutdownMode.GRACEFUL)
        }
    }

    @Test
    fun `node should reach ONLINE state after start`() {
        startNodeAndWaitOnline()
        // Already verified inside helper
    }

    @Test
    fun `node should reach STOPPED state after graceful shutdown`() {
        startNodeAndWaitOnline()

        instance.close(ShutdownMode.GRACEFUL)

        await().atMost(5, TimeUnit.SECONDS)
            .pollInterval(50, TimeUnit.MILLISECONDS)
            .untilAsserted {
                Assertions.assertEquals(
                    NodeState.STOPPED,
                    instance.cluster.state(),
                    "Node did not reach STOPPED state after graceful shutdown"
                )
            }
    }

    @AfterEach
    fun teardown() {
        try {
            instance.close(ShutdownMode.GRACEFUL)

            await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .untilAsserted {
                    val state = instance.cluster.state()
                    Assertions.assertTrue(
                        state == NodeState.STOPPED || state == NodeState.OFFLINE,
                        "Node state must be STOPPED or OFFLINE but was $state"
                    )
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            launchConfig.rootDir.deleteComplete()
        }
    }

    /**
     * Helper function to start the node and wait until it reaches ONLINE state.
     *
     * @param timeoutSeconds Maximum time to wait for node to become ONLINE
     */
    private fun startNodeAndWaitOnline(timeoutSeconds: Long = 5) {
        instance.start()
        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
            .pollInterval(50, TimeUnit.MILLISECONDS)
            .untilAsserted {
                Assertions.assertEquals(
                    NodeState.ONLINE,
                    instance.cluster.state(),
                    "Node did not reach ONLINE state within $timeoutSeconds seconds"
                )
            }
    }
}