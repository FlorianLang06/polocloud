package de.polocloud.node.launch

import de.polocloud.node.NodeInstance

fun main(args: Array<String>) {
    val launchConfig = _root_ide_package_.de.polocloud.node.launch.NodeLaunchConfigParser.parse(args)
    val instance = _root_ide_package_.de.polocloud.node.NodeInstance(launchConfig)

    instance.start()
}
