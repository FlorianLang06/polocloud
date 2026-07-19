package de.polocloud.node.services.factory.process

import java.io.File

/**
 * Default [PlatformRuntime] for Java-based platforms.
 *
 * Explicit [jvmArgs] are always placed before `-jar`. In addition, [args] are
 * split into JVM flags and program arguments: flags matching known JVM prefixes
 * (`-X`, `-D`, `--enable-`, `--add-`, `--patch-`) are inserted before `-jar`;
 * all remaining args are appended after the JAR path.
 *
 * Resulting command structure:
 * ```
 * {executable} [jvmArgs] [globalJvmFlags] -jar <jarFile> [programArgs]
 * ```
 */
object JavaRuntime : PlatformRuntime {

    override val language = "JAVA"

    override fun buildCommand(executable: String?, artifact: File, args: List<String>): List<String> {
        val exec = executable ?: "java"
        val (globalJvmArgs, programArgs) = args.partition { arg ->
            arg.startsWith("-X") || arg.startsWith("-D") ||
            arg.startsWith("--enable-") || arg.startsWith("--add-") || arg.startsWith("--patch-")
        }
        return listOf(exec) + globalJvmArgs + listOf("-jar", artifact.absolutePath) + programArgs
    }
}
