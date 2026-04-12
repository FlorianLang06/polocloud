package de.polocloud.node.bootstrap.properties

import de.polocloud.common.Address
import de.polocloud.node.communication.registration.node.RegistrationInfo
import java.nio.file.Path

data class NodeProperties (
    val rootDir: Path,
    val address: Address? = null,
    // if the node should register itself to a cluster, this token is required to authenticate the node
    val clusterRegistration : RegistrationInfo? = null,
    val group: String
) {

    init {
        System.setProperty("rootDir", rootDir.toString())
    }

    val localPath: Path
        get() = rootDir.resolve("local")

    val localDataPath: Path
        get() = localPath.resolve("data")

    val localNodePath: Path
        get() = rootDir.resolve("local-node.json")

    val localSecurityPath: Path
        get() = localDataPath.resolve(".security")

}