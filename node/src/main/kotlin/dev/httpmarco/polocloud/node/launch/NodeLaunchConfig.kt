package dev.httpmarco.polocloud.node.launch

import java.nio.file.Path
import kotlin.io.path.Path

data class NodeLaunchConfig(
    val localPath: Path
) {

    val localDataPath: Path
        get() = localPath.resolve("data")

    val localNodePath: Path
        get() = Path("local-node.json")

    val localSecurityPath: Path
        get() = localDataPath.resolve(".security")
}