package de.polocloud.node.services.templates

import de.polocloud.common.configuration.ConfigSection
import de.polocloud.common.files.DirectoryScanner
import de.polocloud.common.i18n.trInfo
import de.polocloud.node.services.templates.configurations.ServiceTemplateConfiguration
import org.slf4j.LoggerFactory
import java.nio.file.Path

class TemplateLoader(
    private val templatesDir: Path
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val validator = TemplateValidator()
    private val builder = TemplateBuilder()

    fun load(): List<ServiceTemplate> {
        return DirectoryScanner
            .listDirectories(templatesDir)
            .mapNotNull { loadTemplateSafely(it) }
    }

    private fun loadTemplateSafely(dir: Path): ServiceTemplate? {
        return runCatching {
            loadTemplate(dir)
        }.onFailure {
            logger.trInfo(
                "node",
                "node.services.templates.loader.skipped",
                "fileName" to dir.fileName,
                "message" to it.message
            )
        }.getOrNull()
    }

    private fun loadTemplate(dir: Path): ServiceTemplate {
        val config = loadConfig(dir)

        validator.validate(config, dir)

        return builder.build(config, dir)
    }

    private fun loadConfig(dir: Path): ServiceTemplateConfiguration {
        val configFile = dir.resolve("template.json")
        val section = ConfigSection(configFile)

        return section.read(ServiceTemplateConfiguration.serializer())
            ?: throw IllegalStateException("template.json missing or invalid in $dir")
    }
}