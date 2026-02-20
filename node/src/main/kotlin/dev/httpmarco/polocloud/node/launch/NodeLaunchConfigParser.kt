package dev.httpmarco.polocloud.node.launch

import java.nio.file.Path

object NodeLaunchConfigParser {

    fun parse(args: Array<String>): NodeLaunchConfig {
        val map = args
            .filter { it.startsWith("--") }
            .associate {
                val (key, value) = it.removePrefix("--").split("=", limit = 2)
                key to value
            }

        return NodeLaunchConfig(rootDir = map["directory"]?.let { Path.of(it) } ?: Path.of(""))
    }
}