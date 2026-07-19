package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

/**
 * Terminal argument that resolves a platform role, i.e.
 * [de.polocloud.node.services.factory.platform.Platform.type]: "SERVER" or "PROXY".
 *
 * Accepted case-insensitively, always normalized to uppercase in [buildResult].
 */
class PlatformTypeArgument(key: String) : TerminalArgument<String>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return VALUES.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return VALUES.any { it.equals(rawInput, ignoreCase = true) }
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.platform.type.invalid"
    }

    override fun buildResult(input: String, context: InputContext): String {
        return input.uppercase()
    }

    companion object {
        private val VALUES = listOf("SERVER", "PROXY")
    }
}
