package de.polocloud.node.bootstrap

import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.NodeInstance
import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.bootstrap.properties.NodePropertiesParser
import de.polocloud.node.core.NodeRuntime
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.environment.NodeEnvironment
import org.slf4j.LoggerFactory

class NodeLaunch(
    args: Array<String> = emptyArray(),
    val launchProperties: NodeProperties = NodePropertiesParser.parse(args)
) {

    //load the logger here for faster startup
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        System.setProperty("PID", ProcessHandle.current().pid().toString())

        if (PolocloudVersion.CURRENT.isDebugEnabled) {
            System.setProperty("org.apache.logging.log4j.level", "DEBUG")
        }
    }

    fun run(): NodeInstance {
        val holder = ConfigurationManager.load<NodeConfigurations>()

        val runtime = NodeRuntime(launchProperties, holder)
        val instance = NodeInstance(runtime)

        NodeEnvironment.init(instance)

        instance.initialize()
        return instance
    }
}