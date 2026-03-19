package de.polocloud.node.services.templates

import de.polocloud.node.services.templates.configurations.ServiceTemplateConfiguration
import java.nio.file.Path

class TemplateBuilder {

    fun build(
        config: ServiceTemplateConfiguration,
        directory: Path
    ): ServiceTemplate {

        val jarPath = directory.resolve(config.mainJar)

        return ServiceTemplate(
            name = config.name,
            directory = directory,
            jarPath = jarPath,
            configuration = config
        )
    }
}