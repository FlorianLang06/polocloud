package de.polocloud.node.services

import de.polocloud.node.event.ClusterEventService
import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.ping.ServicePingFactory
import de.polocloud.node.services.queue.ServiceQueue
import de.polocloud.shared.event.server.ServerStoppedEvent
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider(nodePort: Int = 4241) {

    // Concurrent by design: the queue and prune threads mutate this list while API
    // handlers iterate it. A plain ArrayList would risk ConcurrentModificationException
    // and torn reads; CopyOnWriteArrayList gives every reader a stable snapshot.
    val localServices = CopyOnWriteArrayList<LocalService>()

    val platformService = PlatformService()
    private val factory = FactoryService(platformService, this, nodePort)
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

    fun find(name: String) : Service? {
        return ServiceRepository.find(name)
    }

    fun findAll() = ServiceRepository.findAll()

    fun exists(name: String) = ServiceRepository.exists(name)

    fun update(service: Service) {
        ServiceRepository.save(service)
    }
}
