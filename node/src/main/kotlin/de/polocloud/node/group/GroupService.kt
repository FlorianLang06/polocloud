package de.polocloud.node.group

import de.polocloud.node.event.ClusterEventService
import de.polocloud.shared.event.group.GroupUpdatedEvent
import de.polocloud.shared.property.Properties
import org.slf4j.LoggerFactory

open class GroupService {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)

    fun run() {
        logger.info("Found {} groups", GroupRepository.count())
    }

    open fun findAll() = GroupRepository.findAll()

    open fun exists(name: String) = GroupRepository.exists(name)

    open fun find(name: String) = GroupRepository.find(name)

    fun create(name: String, memory: Int, startThreshold: Double, minOnline: Long, maxOnline: Long, platform: String, version: String) : Group {
        val group = Group(name, memory, startThreshold, minOnline, maxOnline, platform, version)

        GroupRepository.save(group)
        return group
    }

    open fun create(group: Group): Group {
        GroupRepository.save(group)
        return group
    }

    open fun update(group: Group): Group {
        GroupRepository.save(group)
        // Notify consumers (e.g. the bridge's fallback tracking) of the new state live,
        // regardless of whether the update came from gRPC or the node terminal.
        ClusterEventService.call(GroupUpdatedEvent(group.name, Properties.of(group.properties)))
        return group
    }

    fun list() = GroupRepository.findAll()

    open fun delete(group: Group) {

        // TODO shutdown all services on ervery node
        // TODO CLEAN QUEUE

        GroupRepository.delete(group)
    }
}