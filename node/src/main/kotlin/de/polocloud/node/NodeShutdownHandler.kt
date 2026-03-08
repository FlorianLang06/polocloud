package de.polocloud.node

import de.polocloud.common.ShutdownMode

class NodeShutdownHandler(val instance: de.polocloud.node.NodeInstance) {

    var running = false

    init {
        this.registerShutdownHook()
    }

    fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            if (running) return@Thread
            running = true
            instance.close(ShutdownMode.FORCE)
        })
    }
}