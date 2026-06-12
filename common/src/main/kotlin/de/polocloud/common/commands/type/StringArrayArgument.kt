package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument

class StringArrayArgument(key: String) : TerminalArgument<String>(key) {

    override fun buildResult(input: String, context: InputContext): String {
        return input
    }
}