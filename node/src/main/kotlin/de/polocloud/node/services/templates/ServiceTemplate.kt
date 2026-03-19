package de.polocloud.node.services.templates

import de.polocloud.node.services.templates.configurations.ServiceTemplateConfiguration
import java.nio.file.Path

data class ServiceTemplate(
    val name: String,
    val directory: Path,
    val jarPath: Path,
    val configuration: ServiceTemplateConfiguration,
) {
    val version: String get() = configuration.version
}