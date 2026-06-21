package de.polocloud.node.terminal.impl

import de.polocloud.common.commands.Command
import de.polocloud.common.commands.type.DoubleArgument
import de.polocloud.common.commands.type.IntArgument
import de.polocloud.common.commands.type.KeywordArgument
import de.polocloud.common.commands.type.LongArgument
import de.polocloud.common.commands.type.TextArgument
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.group.GroupService
import de.polocloud.node.terminal.types.GroupArgument
import org.slf4j.LoggerFactory

class GroupCommand(val groupService: GroupService) : Command("group", "Manage all group things here") {

    private val logger = LoggerFactory.getLogger(GroupCommand::class.java)

    init {
        val groupArgument = GroupArgument("name", groupService)
        val nameArgument = TextArgument("name")
        val memoryArgument = IntArgument("memory")
        val startThresholdArgument = DoubleArgument("startThreshold")
        val maxOnlineArgument = LongArgument("maxOnline")
        val minOnlineArgument = LongArgument("minOnline")

        syntax({
            val name = it.arg(nameArgument)
            val memory = it.arg(memoryArgument)
            val startThreshold = it.arg(startThresholdArgument)
            val maxOnline = it.arg(maxOnlineArgument)
            val minOnline = it.arg(minOnlineArgument)

            if (groupService.exists(name)) {
                logger.trInfo("node", "node.command.group.alreadyExists", Pair("name", name));
                return@syntax
            }

            groupService.create(name, memory, startThreshold, minOnline, maxOnline, "velocity", "3.5.0-SNAPSHOT")
            logger.trInfo("node", "node.command.group.created", Pair("name", name))
        }, "Create a new group", KeywordArgument("create"), nameArgument, memoryArgument, startThresholdArgument, maxOnlineArgument, minOnlineArgument)

        syntax({
            val group = it.arg(groupArgument)
            groupService.delete(group)
            logger.trInfo("node", "node.command.group.deleted", Pair("name", group.name))
        }, "Delete a group", KeywordArgument("delete"), groupArgument)
    }
}