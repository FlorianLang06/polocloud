package dev.httpmarco.polocloud.node.launch

import java.nio.file.Path

data class NodeLaunchConfig(
    val rootDir: Path,
) {

    val localPath: Path
        get() = rootDir.resolve("local")

    val localDataPath: Path
        get() = localPath.resolve("data")

    val localNodePath: Path
        get() = rootDir.resolve("local-node.json")

    val localSecurityPath: Path
        get() = localDataPath.resolve(".security")
}