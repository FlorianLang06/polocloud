package de.polocloud.node.services.templates

import de.polocloud.common.i18n.trInfo
import org.slf4j.LoggerFactory
import java.nio.file.Path

class ServiceTemplateFactory(
    private val templatesDir: Path,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val registry = TemplateRegistry()
    private val loader = TemplateLoader(templatesDir)

    init {
        logger.trInfo("node", "node.services.templates.start.initiating")

        val templates = loader.load()
        registry.registerAll(templates)

        logger.trInfo("node", "node.services.templates.start.success", "size" to templates.size)
    }
}