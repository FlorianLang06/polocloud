package de.polocloud.node.services.templates

import de.polocloud.node.services.templates.configurations.ServiceTemplateConfiguration
import de.polocloud.node.services.templates.exception.InvalidTemplateException
import java.nio.file.Files
import java.nio.file.Path

class TemplateValidator {

    fun validate(config: ServiceTemplateConfiguration, dir: Path) {

        validateName(config, dir)
        validateVersion(config, dir)
        validateJar(config, dir)
        validateRuntime(config, dir)
    }

    private fun validateName(config: ServiceTemplateConfiguration, dir: Path) {
        if (config.name.isBlank()) {
            throw InvalidTemplateException("Template name is empty in $dir")
        }
    }

    private fun validateVersion(config: ServiceTemplateConfiguration, dir: Path) {
        if (config.version.isBlank()) {
            throw InvalidTemplateException("Version is missing in $dir")
        }

        // simple version check (1.0.0 etc.)
        val regex = Regex("""\d+\.\d+\.\d+""")

        if (!regex.matches(config.version)) {
            throw InvalidTemplateException("Invalid version format in $dir: ${config.version}")
        }
    }

    private fun validateJar(config: ServiceTemplateConfiguration, dir: Path) {
        val jarPath = dir.resolve(config.mainJar)

        if (!Files.exists(jarPath)) {
            throw InvalidTemplateException("Main jar not found: $jarPath")
        }

        if (!config.mainJar.endsWith(".jar")) {
            throw InvalidTemplateException("mainJar must be a .jar file in $dir")
        }
    }

    private fun validateRuntime(config: ServiceTemplateConfiguration, dir: Path) {
        if (config.runtime.memory <= 0) {
            throw InvalidTemplateException("Invalid memory setting in $dir")
        }

        if (config.runtime.jvmArgs.isEmpty()) {
            throw InvalidTemplateException("jvmArgs must not be empty in $dir")
        }
    }
}