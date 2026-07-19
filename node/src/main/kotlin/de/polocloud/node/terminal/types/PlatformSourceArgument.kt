package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.services.factory.platform.PlatformVersionSource

/**
 * Terminal argument that resolves which of the two ways a custom platform version can be
 * sourced the operator picked — see [PlatformVersionSource].
 */
class PlatformSourceArgument(key: String) : TerminalArgument<PlatformVersionSource>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return mutableListOf("url", "local")
    }

    override fun predication(rawInput: String): Boolean {
        return rawInput.equals("url", ignoreCase = true) || rawInput.equals("local", ignoreCase = true)
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.platform.source.invalid"
    }

    override fun buildResult(input: String, context: InputContext): PlatformVersionSource {
        return if (input.equals("url", ignoreCase = true)) PlatformVersionSource.URL else PlatformVersionSource.LOCAL_FILE
    }
}
