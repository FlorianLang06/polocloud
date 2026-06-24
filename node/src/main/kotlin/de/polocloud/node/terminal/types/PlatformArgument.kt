package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.Platform

/**
 * Terminal argument that resolves a loaded [Platform] by its name.
 *
 * The raw input is the platform name (e.g. "velocity", "paper"). The argument only
 * matches when a platform with that name is loaded and offers all known platform
 * names as tab-completion suggestions.
 */
class PlatformArgument(
    key: String,
    private val platformService: PlatformService
) : TerminalArgument<Platform>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return platformService.all().map { it.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return platformService.find(rawInput) != null
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.platform.notExists"
    }

    override fun buildResult(input: String, context: InputContext): Platform {
        // safe: predication guarantees the platform is loaded
        return platformService.find(input)!!
    }
}
