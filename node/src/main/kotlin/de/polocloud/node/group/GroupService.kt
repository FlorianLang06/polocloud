package de.polocloud.node.group

import org.slf4j.LoggerFactory

open class GroupService {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)
    private var groupRepository = GroupRepository()

    fun run() {
        logger.info("Found {} groups", groupRepository.count())
    }

    open fun findAll() = groupRepository.findAll()

    open fun exists(name: String) = groupRepository.exists(name)

    open fun find(name: String) = groupRepository.find(name)

    fun create(name: String, memory: Int, startThreshold: Double, minOnline: Long, maxOnline: Long, platform: String, version: String) : Group {
        val group = Group(name, memory, startThreshold, minOnline, maxOnline, platform, version)

        groupRepository.save(group)
        return group
    }

    open fun create(group: Group): Group {
        groupRepository.save(group)
        return group
    }

    open fun update(group: Group): Group {
        groupRepository.save(group)
        return group
    }

    fun list() = groupRepository.findAll()

    open fun delete(group: Group) {

        // TODO shutdown all services on ervery node
        // TODO CLEAN QUEUE

        groupRepository.delete(group)
    }
}