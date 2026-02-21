package dev.httpmarco.polocloud.cli.command.arguments.type

import dev.httpmarco.polocloud.cli.command.arguments.InputContext
import dev.httpmarco.polocloud.cli.command.arguments.TerminalArgument

class StringArrayArgument(key: String) : TerminalArgument<String>(key) {

    override fun buildResult(input: String, context: InputContext): String {
        return input
    }
}