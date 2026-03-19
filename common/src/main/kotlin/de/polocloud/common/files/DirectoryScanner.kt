package de.polocloud.common.files

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

object DirectoryScanner {

    fun listDirectories(root: Path): List<Path> {
        if (!Files.exists(root)) return emptyList()

        return Files.list(root).use { stream ->
            stream
                .filter { Files.isDirectory(it) }
                .collect(Collectors.toList())
        }
    }
}