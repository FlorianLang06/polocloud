package de.polocloud.node.bootstrap

import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.NodeInstance
import de.polocloud.node.bootstrap.properties.NodeProperties
import de.polocloud.node.bootstrap.properties.NodePropertiesParser
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.core.NodeRuntime
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

class NodeLaunch(
    args: Array<String> = emptyArray(),
    val launchProperties: NodeProperties = NodePropertiesParser.parse(args)
) {

    init {
        System.setProperty("PID", ProcessHandle.current().pid().toString())

        if (PolocloudVersion.CURRENT.isDebugEnabled) {
            Configurator.setRootLevel(Level.DEBUG);
        }
    }

    fun run(): NodeInstance {
        val configurations = loadConfigurations(launchProperties)

        val runtime = NodeRuntime(launchProperties, configurations)
        val instance = NodeInstance(runtime)

        NodeEnvironment.init(instance)

        instance.initialize()
        return instance
    }


    /**
     * Loads the node configuration from disk.
     *
     * If no configuration file exists, a default configuration
     * will be created and persisted automatically.
     */
    private fun loadConfigurations(launchProperties: NodeProperties): NodeConfigurations {
        val root = launchProperties.rootDir

        return ConfigurationManager
            .load<NodeConfigurations>()
            .atPath(root.resolve("config.json").toString())
            .value
    }
}