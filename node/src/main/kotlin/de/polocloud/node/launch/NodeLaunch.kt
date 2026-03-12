package de.polocloud.node.launch

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.NodeInstance
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

fun main(args: Array<String>) {
    val launchConfig = NodeLaunchConfigParser.parse(args)

    if (PolocloudVersion.CURRENT.isDebugEnabled) {
        Configurator.setRootLevel(Level.DEBUG);
    }

    val instance = NodeInstance(launchConfig)
    instance.start()
}
