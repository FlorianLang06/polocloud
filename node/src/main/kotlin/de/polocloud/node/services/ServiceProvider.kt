package de.polocloud.node.services

import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.queue.ServiceQueue

class ServiceProvider(nodePort: Int = 4241) {

    val localServices = ArrayList<LocalService>()

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
