package de.polocloud.node.utils

import java.nio.file.Path
import kotlin.io.path.Path

private const val ROOT_DIR_PROPERTY = "rootDir"

fun rootDir() : Path {
    return Path(System.getProperty(ROOT_DIR_PROPERTY))
}

fun rootDir(path: Path)  {
    System.setProperty(ROOT_DIR_PROPERTY, path.toString())
}