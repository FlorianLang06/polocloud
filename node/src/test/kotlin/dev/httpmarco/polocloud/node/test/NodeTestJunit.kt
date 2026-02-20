package dev.httpmarco.polocloud.node.test

import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.io.path.Path

class NodeTestJunit {

    @Test
    @DisplayName("Node should initialize and be online")
    fun run() {
        val localPath = Path("local/${UUID.randomUUID()}")

        val launchConfig = NodeLaunchConfig(localPath)

        val instance = NodeInstance(launchConfig)
        instance.start()

        assert(instance.cluster.state() == NodeState.ONLINE)
    }

}