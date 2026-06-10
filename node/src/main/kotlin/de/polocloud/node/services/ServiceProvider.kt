package de.polocloud.node.services

import de.polocloud.node.group.GroupRepository
import de.polocloud.node.services.factory.FactoryService
import de.polocloud.node.services.queue.ServiceQueue

class ServiceProvider {

    private val factory = FactoryService()
    private val queue = ServiceQueue(factory, GroupRepository())

    fun run() {
        queue.run()
    }
}
