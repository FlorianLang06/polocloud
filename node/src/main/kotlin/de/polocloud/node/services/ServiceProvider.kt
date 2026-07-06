package de.polocloud.node.services

import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.queue.ServiceQueue
import java.util.concurrent.CopyOnWriteArrayList

class ServiceProvider(nodePort: Int = 4241) {

    // Concurrent by design: the queue and prune threads mutate this list while API
    // handlers iterate it. A plain ArrayList would risk ConcurrentModificationException
    // and torn reads; CopyOnWriteArrayList gives every reader a stable snapshot.
    val localServices = CopyOnWriteArrayList<LocalService>()

    val platformService = PlatformService()
    private val factory = FactoryService(platformService, this, nodePort)
    private val queue = ServiceQueue(factory, GroupRepository())

    fun run() {
        platformService.load()
        queue.run()
    }

    fun shutdown() {
        runCatching { queue.close() }

        // Isolate each service: one that hangs or throws must not stop the rest
        // from being terminated.
        this.localServices.forEach { service ->
            runCatching { service.shutdown() }
        }
        this.localServices.clear()
    }
}
