package de.polocloud.node.launch

import de.polocloud.node.NodeInstance

fun main(args: Array<String>) {
    val launchConfig = NodeLaunchConfigParser.parse(args)
    val instance = NodeInstance(launchConfig)

    instance.start()
}
