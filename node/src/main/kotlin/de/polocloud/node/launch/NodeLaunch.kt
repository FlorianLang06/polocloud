package de.polocloud.node.launch

import de.polocloud.common.configuration.ConfigSection
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseCredentials
import de.polocloud.node.NodeInstance
import de.polocloud.node.configuration.NodeConfiguration
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

    fun run() = NodeInstance(launchProperties, loadConfiguration(launchProperties))


    /**
     * Loads the node configuration from disk.
     *
     * If no configuration file exists, a default configuration
     * will be created and persisted automatically.
     */
    private fun loadConfiguration(launchProperties: NodeLaunchProperties): NodeConfiguration {
        return ConfigSection(launchProperties.localNodePath).readOrCreate(
            NodeConfiguration.serializer(),
            NodeConfiguration(
                database = DatabaseCredentials.H2(
                    launchProperties.localDataPath.toString() + "/polocloud.h2.db"
                )
            )
        )
    }
}