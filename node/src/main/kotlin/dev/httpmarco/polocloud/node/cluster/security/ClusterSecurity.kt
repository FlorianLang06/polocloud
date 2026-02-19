package dev.httpmarco.polocloud.node.cluster.security

import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.common.utils.toUUID
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.LOCAL_SECURITY_PATH
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.attribute.DosFileAttributeView
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Manages cluster security for the local node.
 *
 * This class provides persistent storage of:
 * - a cluster-wide token [clusterToken]
 * - a local node identifier [localId]
 *
 * Both values are stored in a local file defined by [LOCAL_SECURITY_PATH].
 * If the file is missing or corrupted, new UUIDs are generated and persisted.
 */
class ClusterSecurity {

    private val logger = LoggerFactory.getLogger(ClusterSecurity::class.java)

    companion object {
        private const val UUID_SIZE = 16
        private const val EXPECTED_FILE_SIZE = UUID_SIZE * 2
    }

    /** Cluster-wide token shared across all nodes in the cluster. */
    val clusterToken: UUID

    /** Unique identifier for this local node. */
    val localId: UUID

    init {
        val (token, id) = loadOrCreate()
        clusterToken = token
        localId = id
    }

    /**
     * Loads the cluster and local IDs from the security file,
     * or generates new ones if the file does not exist or is corrupted.
     *
     * @return a pair of [clusterToken] and [localId]
     */
    private fun loadOrCreate(): Pair<UUID, UUID> {
        return readBytesFromFile() ?: regenerate()
    }

    /**
     * Reads the security file and converts its bytes into UUIDs.
     *
     * @return a pair of [clusterToken] and [localId], or null if the file is missing/corrupted
     */
    private fun readBytesFromFile(): Pair<UUID, UUID>? {
        if (!LOCAL_SECURITY_PATH.exists()) return null

        val bytes = LOCAL_SECURITY_PATH.readBytes()
        if (bytes.size != EXPECTED_FILE_SIZE) return null

        return try {
            val clusterBytes = bytes.copyOfRange(0, UUID_SIZE)
            val localBytes = bytes.copyOfRange(UUID_SIZE, EXPECTED_FILE_SIZE)
            clusterBytes.toUUID() to localBytes.toUUID()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates new UUIDs for [clusterToken] and [localId] and persists them to disk.
     *
     * @return a pair of newly generated [clusterToken] and [localId]
     */
    private fun regenerate(): Pair<UUID, UUID> {
        val parent = LOCAL_SECURITY_PATH.parent ?: throw IllegalStateException("LOCAL_SECURITY_PATH must have a parent directory")
        parent.createDirectories()

        val clusterToken = UUID.randomUUID()
        val localId = UUID.randomUUID()

        writeBytesToFile(clusterToken, localId)

        logger.info(TranslationService.tr("cluster", "cluster.security.regenerated"))
        return clusterToken to localId
    }

    /**
     * Writes the given [clusterToken] and [localId] as bytes to the security file.
     *
     * @param clusterToken the cluster-wide token to persist
     * @param localId the local node identifier to persist
     */
    /**
     * Writes the given [clusterToken] and [localId] as bytes to the security file.
     *
     * The file will also be marked as hidden on Windows.
     *
     * @param clusterToken the cluster-wide token to persist
     * @param localId the local node identifier to persist
     */
    private fun writeBytesToFile(clusterToken: UUID, localId: UUID) {
        val combined = clusterToken.toBytes() + localId.toBytes()
        LOCAL_SECURITY_PATH.writeBytes(combined)

        try {
            val dosView = Files.getFileAttributeView(LOCAL_SECURITY_PATH, DosFileAttributeView::class.java)
            dosView?.setHidden(true)
        } catch (_: Exception) {
            // Ignore errors when trying to set hidden attribute, as it's not critical and may not be supported onall platforms
        }
    }
}
