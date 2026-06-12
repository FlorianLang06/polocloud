package de.polocloud.node.group

import org.slf4j.LoggerFactory

class GroupService {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)
    private var groupRepository = GroupRepository()

    fun run() {
        logger.info("Initializing GroupService...")

        // todo: remove only testing
        if(groupRepository.count().toInt() == 0) {
            val group = Group("proxy", 512, 100.0, 1, 1, "velocity", "3.5.0-SNAPSHOT")
            groupRepository.save(group);
        }

        logger.info("Found {} groups", groupRepository.count())
    }

    fun list() = groupRepository.findAll()
}