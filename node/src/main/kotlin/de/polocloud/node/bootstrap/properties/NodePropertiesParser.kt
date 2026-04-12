package de.polocloud.node.bootstrap.properties

import de.polocloud.common.Address
import de.polocloud.common.system.PolocloudSystemProperties
import de.polocloud.node.communication.registration.node.RegistrationInfo
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
object NodePropertiesParser {

    /**
     * Parses the given command-line arguments into a [NodeProperties].
     *
     * @param args array of CLI arguments, e.g., ["--hostname=localhost", "--port=25565"]
     * @return a [NodeProperties] with values from the arguments or sensible defaults
     * @throws IllegalArgumentException if an argument is malformed (missing `=` separator)
     */
    fun parse(args: Array<String>): NodeProperties {
        val map = args
            .filter { it.startsWith("--") }
            .associate { arg ->
                val stripped = arg.removePrefix("--")
                val parts = stripped.split("=", limit = 2)
                if (parts.size != 2) {
                    // We throw here, because the error system is not loaded yet
                    throw IllegalArgumentException("Malformed argument: '$arg'. Expected format --key=value")
                }
                parts[0] to parts[1]
            }

        val hostname = map["hostname"] ?: "127.0.0.1"
        val port = map["port"]?.toIntOrNull() ?: 1
        val address = Address(hostname, port)
        val group = map["group"]
            ?: System.getProperty(PolocloudSystemProperties.NODE_GROUP)
            ?: "node"

        val rootDir = map["directory"]?.let { Path.of(it) } ?: Path.of("")

        return NodeProperties(
            rootDir = rootDir,
            address = if(map["hostname"] != null && map["port"] != null ) address else null,
            clusterRegistration = resolveRegistration(),
            group = group
        )
    }

    private fun resolveRegistration(): RegistrationInfo? {
        val token = System.getProperty(PolocloudSystemProperties.JOIN_TOKEN)
        val host = System.getProperty(PolocloudSystemProperties.JOIN_HOST)
        val port = System.getProperty(PolocloudSystemProperties.JOIN_PORT)?.toIntOrNull()

        if (token == null || host == null || port == null) {
            return null
        }

        return RegistrationInfo(
            token = token,
            address = Address(host, port)
        )
    }
}