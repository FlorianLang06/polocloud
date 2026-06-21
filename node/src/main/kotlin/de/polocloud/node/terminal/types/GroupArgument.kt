package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.group.Group
import de.polocloud.node.group.GroupService

/**
 * Terminal argument that resolves an existing [Group] by its name.
 *
 * The raw input is the group name. The argument only matches when a group with that
 * name exists and offers all known group names as tab-completion suggestions.
 */
class GroupArgument(
    key: String,
    private val groupService: GroupService
) : TerminalArgument<Group>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return groupService.list().map { it.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank() && groupService.exists(rawInput)
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.group.notExists"
    }

    override fun buildResult(input: String, context: InputContext): Group {
        // safe: predication guarantees the group exists
        return groupService.find(input)!!
    }
}
