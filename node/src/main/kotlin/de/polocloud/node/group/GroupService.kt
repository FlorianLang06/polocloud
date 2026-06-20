package de.polocloud.node.group

import org.slf4j.LoggerFactory

class GroupService {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)
    private var groupRepository = GroupRepository()

    fun run() {
        logger.info("Found {} groups", groupRepository.count())
    }

    fun exists(name: String) = groupRepository.exists(name)

    fun create(name: String, memory: Int, startThreshold: Double, minOnline: Long, maxOnline: Long, platform: String, version: String) : Group {
        val group = Group(name, memory, startThreshold, minOnline, maxOnline, platform, version)

        groupRepository.save(group)
        return group
    }

    fun list() = groupRepository.findAll()
}