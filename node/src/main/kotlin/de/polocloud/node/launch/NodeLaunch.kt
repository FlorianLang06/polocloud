package de.polocloud.node.launch

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.NodeInstance
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LoggerContext

fun main(args: Array<String>) {
    val launchConfig = NodeLaunchConfigParser.parse(args)

    if (PolocloudVersion.CURRENT.isDebugEnabled) {
        val ctx = LoggerContext.getContext(false) as LoggerContext
        ctx.configuration.rootLogger.level = Level.DEBUG
        ctx.updateLoggers()
    }

    val instance = NodeInstance(launchConfig)
    instance.start()
}
