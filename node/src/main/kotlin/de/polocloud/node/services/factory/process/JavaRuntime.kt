package de.polocloud.node.services.factory.process

import de.polocloud.service.factory.process.PlatformRuntime
import java.io.File

/**
 * Default [PlatformRuntime] for Java-based platforms.
 *
 * Splits [args] into JVM flags and program arguments:
 * flags matching known JVM prefixes (`-X`, `-D`, `--enable-`, `--add-`, `--patch-`)
 * are inserted before `-jar`; all remaining args are appended after the JAR path.
 *
 * Resulting command structure:
 * ```
 * {executable} [jvmArgs] -jar <jarFile> [programArgs]
 * ```
 */
object JavaRuntime : PlatformRuntime {

    override val language = "JAVA"

    override fun buildCommand(executable: String, jarFile: File, args: List<String>): List<String> {
        val (jvmArgs, programArgs) = args.partition { arg ->
            arg.startsWith("-X") || arg.startsWith("-D") ||
            arg.startsWith("--enable-") || arg.startsWith("--add-") || arg.startsWith("--patch-")
        }
        return listOf(executable) + jvmArgs + listOf("-jar", jarFile.absolutePath) + programArgs
    }
}
