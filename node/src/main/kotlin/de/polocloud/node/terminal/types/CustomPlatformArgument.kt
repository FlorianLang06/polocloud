package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.Platform

/**
 * Terminal argument that resolves an existing *custom* platform by name — like
 * [PlatformArgument], but only matches platforms with [Platform.custom] set, so a command that
 * mutates a platform (attaching a version, deleting it) can never accidentally target a
 * built-in one.
 */
class CustomPlatformArgument(
    key: String,
    private val platformService: PlatformService
) : TerminalArgument<Platform>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        return platformService.customPlatforms().map { it.name }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        return platformService.find(rawInput)?.custom == true
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.platform.custom.notExists"
    }

    override fun buildResult(input: String, context: InputContext): Platform {
        // safe: predication guarantees a custom platform with this name is loaded
        return platformService.find(input)!!
    }
}
