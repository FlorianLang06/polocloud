package dev.httpmarco.polocloud.node.launch

import dev.httpmarco.polocloud.node.NodeInstance

fun main(args: Array<String>) {
    val launchConfig = NodeLaunchConfigParser.parse(args)
    val instance = NodeInstance(launchConfig)

    instance.start()
}
