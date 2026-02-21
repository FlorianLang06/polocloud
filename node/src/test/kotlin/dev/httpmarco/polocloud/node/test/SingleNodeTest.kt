package dev.httpmarco.polocloud.node.test

import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.files.deleteComplete
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.junit.jupiter.api.*
import java.util.UUID
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleNodeTest {

    private val launchConfig = NodeLaunchConfig(Path("testing-${UUID.randomUUID()}"))
    private var instance: NodeInstance? = null

    @Test
    @DisplayName("Node should initialize and be online")
    fun run() {
        instance = NodeInstance(launchConfig)

        // Start Node asynchronously
        instance!!.start()

        // Optional: Warten, bis Node tatsächlich online ist
        // z.B. max 5 Sekunden
        val timeout: Duration = 5.seconds
        val start = System.currentTimeMillis()
        while (instance!!.cluster.state() != NodeState.ONLINE &&
            System.currentTimeMillis() - start < timeout.inWholeMilliseconds) {
            Thread.sleep(50)
        }

        Assertions.assertEquals(NodeState.ONLINE, instance!!.cluster.state(), "Node did not reach ONLINE state")

        // Close gracefully in the test
        instance!!.close(ShutdownMode.GRACEFUL)
    }

    @AfterEach
    fun teardown() {
        // Safety: Ensure Node is closed
        instance?.close(ShutdownMode.GRACEFUL)
        instance = null

        launchConfig.rootDir.deleteComplete()
    }
}