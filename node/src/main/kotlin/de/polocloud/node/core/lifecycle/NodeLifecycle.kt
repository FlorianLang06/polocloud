package de.polocloud.node.core.lifecycle

import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.database.DatabaseAccess
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.api.trError
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.bootstrap.time.StartupTimer
import de.polocloud.node.core.NodeRuntime
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.core.context.NodeRuntimeContext
import org.apache.logging.log4j.LogManager
import org.slf4j.LoggerFactory

class NodeLifecycle(
    val holder: ConfigurationHolder<NodeConfigurations>,
    private val runtime: NodeRuntime
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var context: NodeRuntimeContext
        private set

    fun initialize() {
        val props = runtime.launchProperties

        TranslationService.init()
        TranslationService.defaultLanguage(holder.value.general.locale)

        DatabaseAccess.initialize(holder.value.localNode.database)

        if (!DatabaseAccess.connect()) {
            throw IllegalStateException("Database connection failed")
        }

        context = runtime.identityService.resolve(props)

        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown(ShutdownMode.GRACEFUL)
        })
    }

    fun start() {
        val container = context.localNodeContainer

        if (!container.isStarting()) {
            throw IllegalStateException(
                "Node is not in starting state: ${container.state()}"
            )
        }

        context.registrationManager.allowRequests()
        context.serviceHandler.initialize()

        container.markOnline()

        runtime.heartBeatService.startScheduler()

        logger.trInfo(
            "cluster",
            "cluster.node.started",
            "version" to PolocloudVersion.CURRENT.toDisplayString(),
            "time" to StartupTimer.formatted
        )
    }

    fun shutdown(mode: ShutdownMode) {
        val container = context.localNodeContainer

        if (container.isOffline() || container.inShutdownProcess()) {
            return
        }

        logger.trInfo("node", "node.shutdown.stopping")

        container.markStopping()

        safe("heartBeatService") {
            runtime.heartBeatService.stopScheduler()
        }

        safe("registrationManager") {
            context.registrationManager.close(mode)
        }

        safe("localNodeContainer") {
            container.markStopped()
        }

        safe("database") {
            DatabaseAccess.close()
        }

        logger.trInfo("node", "node.shutdown.stopped")
        LogManager.shutdown()
    }

    private fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            logger.trError("node", "node.shutdown.task.error", "task" to name)
        }
    }
}