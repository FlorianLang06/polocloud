package de.polocloud.node.terminal.types

import de.polocloud.common.commands.InputContext
import de.polocloud.common.commands.TerminalArgument
import de.polocloud.node.services.factory.PlatformService
import de.polocloud.node.services.factory.platform.Platform
import de.polocloud.node.services.factory.platform.PlatformVersion

/**
 * Terminal argument that resolves a [PlatformVersion] of the platform selected by the
 * preceding [PlatformArgument] in the same syntax.
 *
 * Tab-completion only suggests versions that actually belong to the chosen platform,
 * so the completions follow the platform picked one argument earlier.
 */
class PlatformVersionArgument(
    key: String,
    private val platformService: PlatformService,
    private val platformArgument: PlatformArgument
) : TerminalArgument<PlatformVersion>(key) {

    override fun defaultArgs(context: InputContext): MutableList<String> {
        val platform = resolvePlatform(context) ?: return mutableListOf()
        return platform.versions.map { it.version }.toMutableList()
    }

    override fun predication(rawInput: String): Boolean {
        // predication has no context, so accept any version offered by a loaded platform;
        // buildResult performs the final, platform-specific validation.
        return platformService.all().any { platform -> platform.versions.any { it.version == rawInput } }
    }

    override fun wrongReason(rawInput: String): String {
        return "node.command.platform.version.notExists"
    }

    override fun buildResult(input: String, context: InputContext): PlatformVersion {
        val platform = resolvePlatform(context)
            ?: error("No platform selected for version '$input'")
        return platform.versions.find { it.version == input }
            ?: error("Version '$input' is not available for platform '${platform.name}'")
    }

    private fun resolvePlatform(context: InputContext): Platform? {
        return if (context.contains(platformArgument)) context.arg(platformArgument) else null
    }
}
