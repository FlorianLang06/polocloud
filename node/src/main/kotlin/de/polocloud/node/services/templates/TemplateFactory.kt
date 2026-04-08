package de.polocloud.node.services.templates

import de.polocloud.node.common.rootDir
import de.polocloud.node.services.ServiceContainer
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object TemplateFactory {

    private val templateCachePath = rootDir().resolve("services").resolve("templates")

    fun generateTemplateIfNotExists(container: ServiceContainer) {
        val path = templateCachePath.resolve(container.name())
        if (!path.exists()) {
            path.createDirectories()
        }
    }
}