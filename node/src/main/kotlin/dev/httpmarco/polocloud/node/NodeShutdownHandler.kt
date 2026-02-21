package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.ShutdownMode

class NodeShutdownHandler(val instance: NodeInstance) {

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