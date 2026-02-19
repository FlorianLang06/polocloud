package dev.httpmarco.polocloud.node.cluster.security

import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.common.utils.toUUID
import dev.httpmarco.polocloud.node.LOCAL_SECURITY_PATH
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class ClusterSecurity {

    val clusterToken: UUID
    val localId: UUID

    init {
        val (token, id) = loadOrCreate()
        clusterToken = token
        localId = id
    }

    private fun loadOrCreate(): Pair<UUID, UUID> {
        if (LOCAL_SECURITY_PATH.exists()) {
            val bytes = LOCAL_SECURITY_PATH.readBytes()

            if (bytes.size == 32) {
                try {
                    val clusterBytes = bytes.copyOfRange(0, 16)
                    val localBytes = bytes.copyOfRange(16, 32)

                    return clusterBytes.toUUID() to localBytes.toUUID()
                } catch (_: Exception) {
                    // corrupted file → regenerate
                }
            }
        }

        return regenerate()
    }

    private fun regenerate(): Pair<UUID, UUID> {
        LOCAL_SECURITY_PATH.parent?.createDirectories()

        val clusterToken = UUID.randomUUID()
        val localId = UUID.randomUUID()

        val combined = clusterToken.toBytes() + localId.toBytes()
        LOCAL_SECURITY_PATH.writeBytes(combined)

        return clusterToken to localId
    }
}
