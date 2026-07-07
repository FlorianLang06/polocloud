package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.group.Group
import de.polocloud.node.services.Service
import de.polocloud.node.services.ServiceProvider

/**
 * Terminal argument that resolves an existing [Group] by its name.
 *
 * The raw input is the group name. The argument only matches when a group with that
 * name exists and offers all known group names as tab-completion suggestions.
 */
class ServiceArgument(
    key: String,
    private val serviceProvider: ServiceProvider
) : TerminalArgument<Service>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return serviceProvider.findAll().map { it.name() }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank() && serviceProvider.exists(rawInput)
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.group.notExists"
    }

    override fun buildResult(input: String, context: InputContext): Service {
        // safe: predication guarantees the group exists
        return serviceProvider.find(input)!!
    }
}
