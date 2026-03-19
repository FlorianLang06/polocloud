package de.polocloud.node.test.services.templates

import java.nio.file.Files
import java.nio.file.Path

object TestUtils {

    fun createTemplate(
        root: Path,
        name: String,
        json: String,
        jarName: String? = null
    ): Path {
        val dir = root.resolve(name)
        Files.createDirectories(dir)

        Files.writeString(dir.resolve("template.json"), json.trimIndent())

        if (jarName != null) {
            Files.createFile(dir.resolve(jarName))
        }

        return dir
    }
}