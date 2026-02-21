package dev.httpmarco.polocloud.node.test

import dev.httpmarco.polocloud.common.ShutdownMode
import dev.httpmarco.polocloud.common.files.deleteComplete
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class ClusterNodeAuthTest {

    private val launchConfig = NodeLaunchConfig(Path("testing-${UUID.randomUUID()}"))
    private lateinit var instance: NodeInstance

    @BeforeTest
    fun setup() {

    }

    @Test
    @DisplayName("Node should initialize and be online")
    fun run() {

    }

    @AfterTest
    fun teardown() {
        instance.close(ShutdownMode.GRACEFUL)
        launchConfig.rootDir.deleteComplete()
    }
}