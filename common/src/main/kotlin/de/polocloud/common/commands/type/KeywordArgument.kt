package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

class KeywordArgument(key: String) : TerminalArgument<String>(key) {
    override fun defaultArgs(context: InputContext): MutableList<String> {
        return listOf(key) as MutableList<String>
    }

    override fun wrongReason(rawInput: String): String {
        return ""
    }

    override fun buildResult(input: String, context: InputContext): String {
        return ""
    }

    override fun predication(rawInput: String): Boolean {
        return super.predication(rawInput) && rawInput.equals(key, ignoreCase = true)
    }
}