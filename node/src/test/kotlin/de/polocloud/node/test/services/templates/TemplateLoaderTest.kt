package de.polocloud.node.test.services.templates

import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.services.templates.TemplateLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

class TemplateLoaderTest {

    @BeforeEach
    fun setup() {
        TranslationService.init()
    }

    @Test
    fun `should load valid template`() {
        val root = Files.createTempDirectory("templates")

        TestUtils.createTemplate(
            root,
            "eventbus",
            """
            {
              "name": "eventbus",
              "mainJar": "event-bus.jar",
              "version": "1.0.0",
              "runtime": {
                "memory": 512,
                "jvmArgs": ["-Xmx512M"]
              },
              "files": {
                "copyAll": true
              }
            }
            """,
            "event-bus.jar"
        )

        val loader = TemplateLoader(root)
        val templates = loader.load()

        assertEquals(1, templates.size)

        val template = templates.first()
        assertEquals("eventbus", template.name)
        assertTrue(template.jarPath.toString().endsWith("event-bus.jar"))
    }

    @Test
    fun `should skip template without jar`() {
        val root = Files.createTempDirectory("templates")

        TestUtils.createTemplate(
            root,
            "broken",
            """
            {
              "name": "broken",
              "mainJar": "missing.jar",
              "version": "1.0.0",
              "runtime": {
                "memory": 512,
                "jvmArgs": ["-Xmx512M"]
              },
              "files": {
                "copyAll": true
              }
            }
            """
        )

        val loader = TemplateLoader(root)
        val templates = loader.load()

        assertTrue(templates.isEmpty())
    }

    @Test
    fun `should skip template with invalid version`() {
        val root = Files.createTempDirectory("templates")

        TestUtils.createTemplate(
            root,
            "bad",
            """
            {
              "name": "bad",
              "mainJar": "bad.jar",
              "version": "invalid",
              "runtime": {
                "memory": 512,
                "jvmArgs": ["-Xmx512M"]
              },
              "files": {
                "copyAll": true
              }
            }
            """,
            "bad.jar"
        )

        val loader = TemplateLoader(root)
        val templates = loader.load()

        assertTrue(templates.isEmpty())
    }

    @Test
    fun `should ignore directories without template json`() {
        val root = Files.createTempDirectory("templates")

        Files.createDirectories(root.resolve("empty"))

        val loader = TemplateLoader(root)
        val templates = loader.load()

        assertTrue(templates.isEmpty())
    }
}