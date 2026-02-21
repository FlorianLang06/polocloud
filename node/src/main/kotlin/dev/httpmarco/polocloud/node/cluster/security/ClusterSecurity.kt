package dev.httpmarco.polocloud.node.cluster.security

import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.common.utils.toUUID
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.launch.NodeLaunchConfig
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.DosFileAttributeView
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Manages the cluster security for a local node.
 *
 * Responsibilities:
 * - Persistent storage of the local node ID and RSA KeyPair
 * - Signing and verifying arbitrary data for cluster authentication
 * - Temporary storage of quorum approval signatures during node join
 *
 * Security:
 * - PrivateKey remains only on the local node
 * - PublicKey can be shared with other nodes to verify signatures
 * - All sensitive data is stored in binary format in a hidden file on disk
 */
class ClusterSecurity(val securityLocalPath: Path) {

    private val logger = LoggerFactory.getLogger(ClusterSecurity::class.java)

    /** Temporary list of quorum approval signatures collected during join process */
    val quorumSignatures = mutableListOf<String>()

    /** Unique identifier of this local node */
    val localId: UUID

    /** Private key of this node */
    val privateKey: PrivateKey

    /** Public key of this node */
    val publicKey: PublicKey

    init {
        val (id, keyPair) = loadOrCreate()
        localId = id
        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    /**
     * Loads the node ID and KeyPair from disk, or generates new ones if missing or corrupted.
     *
     * @return Pair of [UUID] and [KeyPair]
     */
    private fun loadOrCreate(): Pair<UUID, KeyPair> = readFromFile() ?: regenerate()

    /**
     * Reads the local security file and reconstructs UUID and KeyPair.
     *
     * File format:
     * 16 bytes UUID +
     * 4 bytes public key length + public key bytes +
     * 4 bytes private key length + private key bytes
     *
     * @return Pair of [UUID] and [KeyPair], or null if file missing/corrupt
     */
    private fun readFromFile(): Pair<UUID, KeyPair>? {
        if (!securityLocalPath.exists()) return null

        return try {
            val data = securityLocalPath.readBytes()
            var offset = 0

            // UUID
            val uuidBytes = data.copyOfRange(offset, 0 + 16)
            offset += 16
            val localId = uuidBytes.toUUID()

            // PublicKey
            val (publicBytes, offsetAfterPub) = readLengthPrefixedBytes(data, offset)
            offset = offsetAfterPub

            // PrivateKey
            val (privateBytes, _) = readLengthPrefixedBytes(data, offset)

            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(publicBytes))
            val privateKey = keyFactory.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateBytes))

            localId to KeyPair(publicKey, privateKey)
        } catch (e: Exception) {
            logger.warn("Failed to read ClusterSecurity file: ${e.message}")
            null
        }
    }

    /**
     * Generates a new UUID and RSA KeyPair, then persists them to disk.
     *
     * @return Pair of newly generated [UUID] and [KeyPair]
     */
    private fun regenerate(): Pair<UUID, KeyPair> {
        val parent = securityLocalPath.parent ?: throw IllegalStateException("LOCAL_SECURITY_PATH must have a parent directory")
        parent.createDirectories()

        val localId = UUID.randomUUID()
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        writeToFile(localId, keyPair)
        logger.info(TranslationService.tr("cluster", "cluster.security.regenerated"))
        return localId to keyPair
    }

    /**
     * Writes UUID and KeyPair to disk in binary format.
     *
     * Format:
     * [16 bytes UUID][4 bytes pub length][pub bytes][4 bytes priv length][priv bytes]
     *
     * @param localId Node UUID
     * @param keyPair Node KeyPair
     */
    private fun writeToFile(localId: UUID, keyPair: KeyPair) {
        val localIdBytes = localId.toBytes()
        val pubBytes = keyPair.public.encoded
        val privBytes = keyPair.private.encoded

        val data = ByteArray(16 + 4 + pubBytes.size + 4 + privBytes.size)
        var offset = 0

        // UUID
        localIdBytes.copyInto(data, offset)
        offset += 16

        // PublicKey length + bytes
        offset = writeLengthPrefixedBytes(pubBytes, data, offset)
        writeLengthPrefixedBytes(privBytes, data, offset)

        securityLocalPath.writeBytes(data)

        try {
            val dosView = Files.getFileAttributeView(securityLocalPath, DosFileAttributeView::class.java)
            dosView?.setHidden(true)
        } catch (_: Exception) {}
    }

    /**
     * Signs arbitrary data with the node's private key.
     *
     * @param data Bytes to sign
     * @return Base64-encoded signature
     */
    fun sign(data: ByteArray): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        val signedBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signedBytes)
    }

    /**
     * Verifies a signature with a given public key.
     *
     * @param data Original data bytes
     * @param signatureB64 Base64-encoded signature
     * @param pubKey PublicKey to verify with
     * @return true if signature is valid
     */
    fun verify(data: ByteArray, signatureB64: String, pubKey: PublicKey): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(pubKey)
            signature.update(data)
            signature.verify(Base64.getDecoder().decode(signatureB64))
        } catch (_: Exception) {
            false
        }
    }

    private fun readLengthPrefixedBytes(data: ByteArray, startOffset: Int): Pair<ByteArray, Int> {
        var offset = startOffset
        val length = ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
        offset += 4
        val bytes = data.copyOfRange(offset, offset + length)
        offset += length
        return bytes to offset
    }

    private fun writeLengthPrefixedBytes(src: ByteArray, dest: ByteArray, offset: Int): Int {
        var currentOffset = offset
        val len = src.size
        dest[currentOffset++] = ((len shr 24) and 0xFF).toByte()
        dest[currentOffset++] = ((len shr 16) and 0xFF).toByte()
        dest[currentOffset++] = ((len shr 8) and 0xFF).toByte()
        dest[currentOffset++] = (len and 0xFF).toByte()
        src.copyInto(dest, currentOffset)
        currentOffset += len
        return currentOffset
    }
}

/** Convenience Base64 helpers */
fun PublicKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)
fun PrivateKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)
