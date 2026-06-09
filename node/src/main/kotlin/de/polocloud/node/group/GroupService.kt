package de.polocloud.node.group

import org.slf4j.LoggerFactory

class GroupService {

    private var logger = LoggerFactory.getLogger(GroupService::class.java)
    private var groupRepository = GroupRepository()

    fun run() {
        logger.info("Initializing GroupService...")
        logger.info("Found {} groups", groupRepository.count())
    }
}