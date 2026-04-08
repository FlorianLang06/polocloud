package de.polocloud.node.services

import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object ServiceTemplateFactory {

    fun generateTemplateIfNotExists(container: ServiceContainer) {
        val path = container.path()
        if (!path.exists()) {
            path.createDirectories()
        }
    }
}