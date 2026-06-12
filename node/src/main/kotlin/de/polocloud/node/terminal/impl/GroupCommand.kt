package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.node.group.GroupService
import org.slf4j.LoggerFactory

class GroupCommand(val groupService: GroupService) : Command("group", "Manage all group things here") {

    private val logger = LoggerFactory.getLogger(GroupCommand::class.java)

    init {
        syntax({ _ ->
            groupService.list().forEach {
                logger.info(it.name)
            }
        }, "Create a new group", KeywordArgument("list"))
    }

}