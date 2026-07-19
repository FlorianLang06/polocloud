package de.polocloud.node.services.factory.process

import java.io.File

object BinaryRuntime : PlatformRuntime {

    /** Language identifier matching [de.polocloud.node.services.factory.platform.Platform.language]. */
    override val language = "BINARY"

    override fun buildCommand(
        executable: String?,
        artifact: File,
        args: List<String>
    ): List<String> {
        return listOf(artifact.absolutePath) + args
    }

}