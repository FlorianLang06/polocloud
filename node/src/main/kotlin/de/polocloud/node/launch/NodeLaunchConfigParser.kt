package de.polocloud.node.launch

import de.polocloud.common.Address
import java.nio.file.Path

/**
 * Parser for command-line arguments to produce a [de.polocloud.node.launch.NodeLaunchConfig].
 *
 * Supported CLI arguments:
 * --hostname=<hostname>   Optional host for the node (default: empty string)
 * --port=<port>           Optional port for the node (default: -1, meaning unspecified)
 * --directory=<path>      Optional root directory for node data (default: current directory)
 *
 * Example usage:
 * ```
 * val config = NodeLaunchConfigParser.parse(arrayOf("--hostname=localhost", "--port=25565"))
 * ```
 */
object NodeLaunchConfigParser {

    /**
     * Parses the given command-line arguments into a [NodeLaunchConfig].
     *
     * @param args array of CLI arguments, e.g., ["--hostname=localhost", "--port=25565"]
     * @return a [NodeLaunchConfig] with values from the arguments or sensible defaults
     * @throws IllegalArgumentException if an argument is malformed (missing `=` separator)
     */
    fun parse(args: Array<String>): NodeLaunchConfig {
        val map = args
            .filter { it.startsWith("--") }
            .associate { arg ->
                val stripped = arg.removePrefix("--")
                val parts = stripped.split("=", limit = 2)
                if (parts.size != 2) {
                    throw IllegalArgumentException("Malformed argument: '$arg'. Expected format --key=value")
                }
                parts[0] to parts[1]
            }

        val hostname = map["hostname"] ?: ""
        val port = map["port"]?.toIntOrNull() ?: -1
        val address = Address(hostname, port)

        val rootDir = map["directory"]?.let { Path.of(it) } ?: Path.of("")

        return NodeLaunchConfig(
            rootDir = rootDir,
            address = address
        )
    }
}