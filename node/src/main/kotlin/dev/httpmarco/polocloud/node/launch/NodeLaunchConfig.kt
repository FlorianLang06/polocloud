package dev.httpmarco.polocloud.node.launch

import dev.httpmarco.polocloud.common.Address
import java.nio.file.Path

data class NodeLaunchConfig(
    val rootDir: Path,
    val address: Address = Address("-", 1)
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