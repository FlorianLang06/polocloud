package de.polocloud.node.launch

import de.polocloud.common.Address
import de.polocloud.node.registration.RegistrationInfo
import java.nio.file.Path

data class NodeLaunchProperties (
    val rootDir: Path,
    val address: Address? = null,
    // if the node should register itself to a cluster, this token is required to authenticate the node
    val clusterRegistrationToken : RegistrationInfo? = null
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