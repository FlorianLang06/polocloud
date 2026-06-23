package de.polocloud.node.services

import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.queue.ServiceQueue

class ServiceProvider {

    val localServices = ArrayList<LocalService>()

    private val platformService = PlatformService()
    private val factory = FactoryService(platformService, this)
    private val queue = ServiceQueue(factory, GroupRepository())

    fun run() {
        platformService.load()
        queue.run()
    }

    fun shutdown() {
        this.localServices.forEach {
            it.shutdown()
        }
    }
}
