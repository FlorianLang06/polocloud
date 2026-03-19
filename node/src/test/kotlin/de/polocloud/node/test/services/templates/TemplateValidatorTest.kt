package de.polocloud.node.test.services.templates

import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.services.templates.TemplateValidator
import de.polocloud.node.services.templates.configurations.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

class TemplateValidatorTest {

    private val validator = TemplateValidator()

    @BeforeEach
    fun setup() {
        TranslationService.init()
    }

    @Test
    fun `should validate correct config`() {
        val dir = Files.createTempDirectory("template")
        Files.createFile(dir.resolve("test.jar"))

        val config = ServiceTemplateConfiguration(
            name = "test",
            mainJar = "test.jar",
            description = null,
            version = "1.0.0",
            runtime = ServiceRuntimeConfiguration(512, listOf("-Xmx512M")),
            files = ServiceFileConfiguration(true)
        )

        assertDoesNotThrow {
            validator.validate(config, dir)
        }
    }

    @Test
    fun `should fail when jar is missing`() {
        val dir = Files.createTempDirectory("template")

        val config = ServiceTemplateConfiguration(
            name = "test",
            mainJar = "missing.jar",
            description = null,
            version = "1.0.0",
            runtime = ServiceRuntimeConfiguration(512, listOf("-Xmx512M")),
            files = ServiceFileConfiguration(true)
        )

        assertThrows(Exception::class.java) {
            validator.validate(config, dir)
        }
    }

    @Test
    fun `should fail with invalid version`() {
        val dir = Files.createTempDirectory("template")
        Files.createFile(dir.resolve("test.jar"))

        val config = ServiceTemplateConfiguration(
            name = "test",
            mainJar = "test.jar",
            description = null,
            version = "abc",
            runtime = ServiceRuntimeConfiguration(512, listOf("-Xmx512M")),
            files = ServiceFileConfiguration(true)
        )

        assertThrows(Exception::class.java) {
            validator.validate(config, dir)
        }
    }

}