package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.ShutdownMode

class NodeShutdownHandler(val instance: NodeInstance) {

    companion object {
        var shutdownProcess = false;
    }

    fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            if (shutdownProcess) return@Thread
            shutdownProcess = true
            instance.close(ShutdownMode.FORCE)
        })
    }
}