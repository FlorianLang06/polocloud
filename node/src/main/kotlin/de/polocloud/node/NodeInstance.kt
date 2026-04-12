package de.polocloud.node

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.node.core.context.NodeRuntimeContext
import de.polocloud.node.core.NodeRuntime

class NodeInstance(
    val runtime: NodeRuntime
) : Closeable {

    val context: NodeRuntimeContext
        get() = this.runtime.lifecycle.context

    fun initialize() {
        this.runtime.lifecycle.initialize()
    }

    fun start() {
       this.runtime.lifecycle.start()
    }

    override fun close(mode: ShutdownMode) {
        this.runtime.lifecycle.shutdown(mode)
    }
}