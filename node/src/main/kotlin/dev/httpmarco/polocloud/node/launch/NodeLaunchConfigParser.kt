package dev.httpmarco.polocloud.node.launch

import dev.httpmarco.polocloud.common.Address
import java.nio.file.Path

object NodeLaunchConfigParser {

    fun parse(args: Array<String>): NodeLaunchConfig {
        val map = args
            .filter { it.startsWith("--") }
            .associate {
                val (key, value) = it.removePrefix("--").split("=", limit = 2)
                key to value
            }

        val hostname = map["hostname"] ?: ""
        val port = map["port"]?.toIntOrNull() ?: -1
        val address = Address(hostname, port)

        return NodeLaunchConfig(
            rootDir = map["directory"]?.let { Path.of(it) } ?: Path.of(""),
            address = address
        )
    }
}