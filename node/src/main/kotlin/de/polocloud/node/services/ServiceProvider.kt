package de.polocloud.node.services

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.ping.ServicePingFactory
import de.polocloud.node.services.queue.ServiceQueue
import de.polocloud.shared.event.server.ServerStoppedEvent
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider(
    nodePort: Int = 4241,
    // Host services advertise to the API (the node's reachable hostname).
    nodeHost: String = "127.0.0.1",
    /** Id of the node this provider runs on; attached to services in the API view. */
    val nodeId: String = "",
    /**
     * Shared with [de.polocloud.node.group.GroupService] so both agree on the same
     * loaded platform set (e.g. resolving whether a group's platform is a proxy for its
     * default templates) instead of each loading its own copy.
     */
    val platformService: PlatformService = PlatformService(),
) {

    // Concurrent by design: the queue and prune threads mutate this list while API
    // handlers iterate it. A plain ArrayList would risk ConcurrentModificationException
    // and torn reads; CopyOnWriteArrayList gives every reader a stable snapshot.
    val localServices = CopyOnWriteArrayList<LocalService>()

    private val factory = FactoryService(platformService, this, nodePort, nodeHost)
    private val queue = ServiceQueue(factory, this)

    // Pings starting services and flips them to RUNNING once they are reachable.
    private val pingFactory = ServicePingFactory(this)

    fun run() {
        platformService.load()
        queue.run()
        pingFactory.run()
    }

    fun shutdown() {
        runCatching { pingFactory.close() }
        runCatching { queue.close() }

        // Isolate each service: one that hangs or throws must not stop the rest
        // from being terminated.
        this.localServices.forEach { service ->
            runCatching { service.shutdown() }
        }
        this.localServices.clear()
    }

    /**
     * Finds a service by its cluster-wide `group-index` [name] (e.g. `lobby-1`).
     *
     * Not a repository `findById`: the persisted identifier is the service UUID, so
     * looking a service up by name means scanning by [Service.name] instead — passing a
     * name to `findById` would hit the UUID id column and fail to convert.
     */
    fun find(name: String) : Service? {
        return findAll().firstOrNull { it.name().equals(name, ignoreCase = true) }
    }

    fun findAll() = ServiceRepository.findAll()

    fun exists(name: String) = find(name) != null

    fun update(service: Service) {
        ServiceRepository.save(service)
    }

    /** Persists the current state of [service] (port/host/state) to the database. */
    fun persist(service: Service) {
        ServiceRepository.save(service)
    }

    /** Removes [service] from the database (e.g. once its process has exited). */
    fun remove(service: Service) {
        ServiceRepository.delete(service)
    }

    /** The live [LocalService] with the given `group-index` [name], or `null` if not running here. */
    fun findLocal(name: String): LocalService? =
        localServices.firstOrNull { it.name().equals(name, ignoreCase = true) }

    /**
     * Shuts down a single running service: terminates the process, removes it from the
     * live list and publishes [ServerStoppedEvent] so the bridge/API drop it immediately
     * (rather than only on the next prune pass).
     */
    fun shutdownLocal(service: LocalService) {
        runCatching { service.shutdown() }
        localServices.remove(service)
        ClusterEventService.call(ServerStoppedEvent(ServiceEventMapper.toShared(service)))
    }

    /**
     * Stops every running service of [groupName] and drops any of its still-queued
     * services. Used when a group is deleted so it leaves no orphaned processes behind.
     */
    fun shutdownGroup(groupName: String) {
        queue.removeGroup(groupName)
        localServices
            .filter { it.groupName.equals(groupName, ignoreCase = true) }
            .forEach { shutdownLocal(it) }
    }
}
