package de.polocloud.node.services.factory

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.forwarding.ForwardingHandler
import de.polocloud.node.group.Group
import de.polocloud.node.services.LocalService
import de.polocloud.node.services.ServiceEventMapper
import de.polocloud.node.services.ServiceProvider
import de.polocloud.shared.service.ServiceState
import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion
import de.polocloud.node.services.factory.process.PlatformProcess
import de.polocloud.node.services.factory.task.TaskExecutor
import de.polocloud.node.security.ServiceIdentityProvisioner
import de.polocloud.shared.event.server.ServerStoppedEvent
import org.slf4j.LoggerFactory
import java.io.File

class FactoryService(
    private val platformService: PlatformService,
    private val serviceProvider: ServiceProvider,
    private val nodePort: Int = 4241,
    // Host services are reachable on / advertise to the API. Derived from the node's
    // configured hostname (see GeneralConfiguration.hostname) so a remote proxy can
    // reach services that are not co-located, instead of a hard-coded 127.0.0.1.
    private val nodeHost: String = "127.0.0.1",
) {

    private val logger = LoggerFactory.getLogger(FactoryService::class.java)

    // Owns the shared player-forwarding secret; the same token is written into every
    // backend server and the proxy so modern forwarding can be established.
    private val forwardingHandler = ForwardingHandler()

    private companion object {
        const val SERVER_BASE_PORT = 30000
        const val PROXY_BASE_PORT = 25565

        // A service always connects *back* to its own node over loopback: it is started
        // by, and co-located with, that node. This is independent of [nodeHost], which is
        // the address the service is *advertised* under to remote consumers.
        const val NODE_BACK_CONNECT_HOST = "127.0.0.1"
    }

    fun start(service: LocalService, group: Group) {
        val platform = platformService.find(group.platform)
            ?: throw IllegalArgumentException("Platform '${group.platform}' is not loaded")
        val version = platform.versions.find { it.version == group.version }
            ?: throw IllegalArgumentException(
                "Version '${group.version}' not available for platform '${group.platform}'"
            )

        val workDir = File("servers/${group.name}-${service.index}")
        val process = PlatformProcess(platform, version)
        val jar = process.download(workDir)

         service.port = assignPort(platform, service.index)
         service.hostname = nodeHost
         service.static = group.static

        // Seed the service's properties from its group so group-level flags (e.g. `fallback`)
        // are visible on the service without overwriting any already set on it.
        group.properties.forEach { (key, value) -> service.properties.putIfAbsent(key, value) }

        installBridgePlugin(platform, workDir)
        applyTasks(platform, version, service, group, workDir)

        val identityDir = File(workDir, "identity/service")
        ServiceIdentityProvisioner.provision(identityDir, service.id.toString(), group.name)

        val proc = process.start(
            jar,
            environment = mapOf(
                "POLOCLOUD_IDENTITY_DIR" to identityDir.absolutePath,
                // Loopback: the service reaches its own node locally, regardless of the
                // host it is advertised under.
                "POLOCLOUD_NODE_HOST" to NODE_BACK_CONNECT_HOST,
                "POLOCLOUD_NODE_PORT" to nodePort.toString(),
            ),
        )

        service.process = proc
        service.workDir = workDir.toPath()
        // Start pumping the process console into the service log buffer so `service <name> logs`
        // can tail it.
        service.startLogCapture()
        // The process is alive but the server is still loading — it only counts as online
        // once ServicePingFactory can reach it, which then flips the state to RUNNING and
        // fires ServerStartedEvent.
        service.state = ServiceState.STARTING
        serviceProvider.localServices.add(service)
        // Persist the now-assigned port/host/state so the database reflects the live
        // service instead of the placeholder row written while it was still queued.
        serviceProvider.persist(service)
        logger.info("Service {}-{} started (pid: {})", group.name, service.index, proc.pid())
    }

    /**
     * Applies the platform's pre-start configuration tasks to the service work directory.
     *
     * Only tasks whose version range matches [version] are applied. Step values may
     * reference placeholders (e.g. `%server_port%` or `%FORWARDING_SECRET%`) which are
     * resolved from the service-specific values built here. The forwarding secret comes
     * from the node-wide [forwardingHandler] so every service and the proxy share it.
     */
    private fun applyTasks(
        platform: Platform,
        version: PlatformVersion,
        service: LocalService,
        group: Group,
        workDir: File,
    ) {
        if (platform.tasks.isEmpty()) return
        TaskExecutor.apply(
            workDir = workDir,
            tasks = platform.tasks,
            version = version.version,
            definitions = platformService.taskDefinitions(),
            placeholders = mapOf(
                "server_port" to service.port.toString(),
                "service_name" to "${group.name}-${service.index}",
                "service_id" to service.id.toString(),
                "group_name" to group.name,
                "FORWARDING_SECRET" to forwardingHandler.secret,
            ),
        )
    }

    /**
     * Derives a deterministic port for a service from its platform role and index.
     *
     * Proxies start at [PROXY_BASE_PORT], all other platforms at [SERVER_BASE_PORT];
     * the 1-based service index is added so co-located services never collide.
     */
    private fun assignPort(platform: Platform, index: Int): Int {
        val base = if (platform.type.equals("PROXY", ignoreCase = true)) PROXY_BASE_PORT else SERVER_BASE_PORT
        return base + (index - 1)
    }

    /**
     * Installs the Polocloud bridge plugin into a proxy's `plugins/` directory.
     *
     * The bridge fat jar is shipped inside the runner and laid out under
     * `.cache/dependencies` on startup. For proxy platforms (Velocity, Waterfall)
     * it is copied into the service work directory so the proxy loads it on boot.
     * Non-proxy platforms are skipped.
     */
    private fun installBridgePlugin(platform: Platform, workDir: File) {
        if (!platform.type.equals("PROXY", ignoreCase = true)) return

        val version = PolocloudVersion.CURRENT.toVersionString()
        val bridgeJar = File(".cache/dependencies/de/polocloud/bridge/$version/bridge-$version.jar")
        if (!bridgeJar.exists()) {
            logger.warn(
                "Bridge plugin not found at {} — proxy starts without the Polocloud bridge",
                bridgeJar.path
            )
            return
        }

        val target = File(workDir, "plugins/polocloud-bridge.jar")
        target.parentFile.mkdirs()
        bridgeJar.copyTo(target, overwrite = true)
        logger.info("  ⇄ Installed Polocloud bridge into {}", target.path)
    }

    fun runningCount(groupName: String): Long {
        pruneDeadProcesses()
        return serviceProvider.localServices.count { it.groupName == groupName }.toLong()
    }

    fun runningIndexes(groupName: String): Set<Int> {
        pruneDeadProcesses()
        return serviceProvider.localServices
            .filter { it.groupName == groupName }
            .map { it.index }
            .toSet()
    }

    private fun pruneDeadProcesses() {
        serviceProvider.localServices.removeIf { service ->
            val dead = service.process?.isAlive != true
            if (dead) {
                // Drop the persisted row too so a crashed/exited service does not linger
                // in the database with a stale state after it is gone.
                serviceProvider.remove(service)
                ClusterEventService.call(ServerStoppedEvent(ServiceEventMapper.toShared(service)))
            }
            dead
        }
    }
}
