package de.polocloud.common.commands.type

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import org.slf4j.LoggerFactory

class TextArgument(key: String) : TerminalArgument<String>(key) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun buildResult(input: String, context: InputContext): String {
        return input
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.isNotBlank()
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.text.empty"
    }
}