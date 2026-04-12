package de.polocloud.node.core.environment

import de.polocloud.node.NodeInstance

object NodeEnvironment {

    lateinit var instance: NodeInstance
        private set

    fun init(instance: NodeInstance) {
        this.instance = instance
    }

    val runtime get() = instance.runtime

}