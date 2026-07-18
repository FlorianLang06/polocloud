package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

class BooleanArgument(key: String) : TerminalArgument<Boolean>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf("yes", "no")
    }

    override fun predication(rawInput: String): Boolean {
        return TRUE_VALUES.contains(rawInput.lowercase()) || FALSE_VALUES.contains(rawInput.lowercase())
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.boolean.invalid"
    }

    override fun buildResult(input: String, context: InputContext): Boolean {
        return TRUE_VALUES.contains(input.lowercase())
    }

    companion object {
        private val TRUE_VALUES = setOf("yes", "y", "true")
        private val FALSE_VALUES = setOf("no", "n", "false")
    }
}