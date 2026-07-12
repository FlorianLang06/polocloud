package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.group.template.GroupTemplateService

/**
 * Terminal argument that resolves an existing template by its folder name.
 *
 * The raw input is the template name. The argument only matches when a folder for that
 * name exists under `local/templates/` and offers all known template names as
 * tab-completion suggestions.
 */
class TemplateArgument(key: String) : TerminalArgument<String>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return GroupTemplateService.listAll().toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank() && GroupTemplateService.directoryOf(rawInput).isDirectory
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.template.notExists"
    }

    override fun buildResult(input: String, context: InputContext): String {
        return input
    }
}