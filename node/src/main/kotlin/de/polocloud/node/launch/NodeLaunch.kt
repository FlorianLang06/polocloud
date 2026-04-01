package de.polocloud.node.launch

import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.NodeInstance
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.configuration.GeneralConfiguration
import de.polocloud.node.configuration.LocalNodeConfiguration
import de.polocloud.node.configuration.NodeConfigurations
import de.polocloud.node.shutdown.ShutdownHook
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

class NodeLaunch(args: Array<String> = emptyArray(), val launchProperties: NodeLaunchProperties = NodeLaunchPropertiesParser.parse(args)) {

    init {
        System.setProperty("PID", ProcessHandle.current().pid().toString())

        // register shutdown hook
        ShutdownHook.registerShutdownHook()

        if (PolocloudVersion.CURRENT.isDebugEnabled) {
            Configurator.setRootLevel(Level.DEBUG);
        }
    }

    fun run() = NodeInstance(launchProperties, loadConfigurations(launchProperties))


    /**
     * Loads the node configuration from disk.
     *
     * If no configuration file exists, a default configuration
     * will be created and persisted automatically.
     */
    private fun loadConfigurations(launchProperties: NodeLaunchProperties): NodeConfigurations {
        val root = launchProperties.rootDir

        return NodeConfigurations(
            cluster = ConfigurationManager
                .load<ClusterConfiguration>()
                .atPath(root.resolve("cluster.json").toString()),

            general = ConfigurationManager
                .load<GeneralConfiguration>()
                .atPath(root.resolve("general.json").toString()),

            localNode = ConfigurationManager
                .load<LocalNodeConfiguration>()
                .atPath(root.resolve("local-node.json").toString()),
        )
    }
}