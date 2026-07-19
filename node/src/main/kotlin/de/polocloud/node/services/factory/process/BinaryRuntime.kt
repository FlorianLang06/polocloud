package de.polocloud.node.services.factory.process

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

object BinaryRuntime : PlatformRuntime {

    /** Language identifier matching [de.polocloud.node.services.factory.platform.Platform.language]. */
    override val language = "BINARY"

    override fun buildCommand(
        executable: String?,
        artifact: File,
        args: List<String>
    ): List<String> {
        if (isPosixCompatible()) {
            val permissions = Files.getPosixFilePermissions(artifact.toPath())
            if (!permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                permissions.add(PosixFilePermission.OWNER_EXECUTE)
                Files.setPosixFilePermissions(artifact.toPath(), permissions)
            }
        }
        return listOf(artifact.absolutePath) + args
    }

    fun isPosixCompatible(): Boolean {
        val supportedViews = FileSystems.getDefault().supportedFileAttributeViews()
        return supportedViews.contains("posix")
    }

}