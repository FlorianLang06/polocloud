package de.polocloud.node.test.services.templates

import de.polocloud.node.services.templates.TemplateBuilder
import de.polocloud.node.services.templates.configurations.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class TemplateBuilderTest {

    private val builder = TemplateBuilder()

    @Test
    fun `should build template correctly`() {
        val dir = Files.createTempDirectory("template")

        val config = ServiceTemplateConfiguration(
            name = "eventbus",
            mainJar = "event-bus.jar",
            description = null,
            version = "1.0.0",
            runtime = ServiceRuntimeConfiguration(512, listOf("-Xmx512M")),
            files = ServiceFileConfiguration(true)
        )

        val template = builder.build(config, dir)

        assertEquals("eventbus", template.name)
        assertEquals(dir.resolve("event-bus.jar"), template.jarPath)
        assertEquals(config, template.configuration)
    }
}