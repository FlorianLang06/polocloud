package dev.httpmarco.polocloud.node.cluster.security

import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.common.utils.toUUID
import dev.httpmarco.polocloud.i18n.api.TranslationService
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.DosFileAttributeView
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import java.util.Base64
import java.util.Date
import java.math.BigInteger
import java.io.File
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Manages the cluster security for a local node with TLS support.
 *
 * Responsibilities:
 * - Generates or loads a persistent RSA key pair.
 * - Creates a self-signed X.509 certificate for TLS usage.
 * - Stores private key and certificate on disk for gRPC TLS.
 * - Signs and verifies arbitrary data for cluster authentication.
 *
 * Security:
 * - PrivateKey remains only on the local node.
 * - PublicKey and Certificate can be shared with other nodes or clients.
 * - All sensitive data is stored in DER binary format in hidden files on disk.
 *
 * TLS Usage:
 * - The generated `node.key` and `node.crt` can be directly used for gRPC TLS.
 */
class ClusterSecurity(val securityLocalPath: Path) {

    private val logger = org.slf4j.LoggerFactory.getLogger(ClusterSecurity::class.java)

    /** Temporary list of quorum approval signatures collected during join process */
    val quorumSignatures = mutableListOf<String>()

    /** Unique identifier of this local node */
    val localId: UUID

    /** Private key of this node */
    val privateKey: PrivateKey

    /** Public key of this node */
    val publicKey: PublicKey

    /** Self-signed X.509 certificate for TLS */
    val certificate: X509Certificate

    init {
        Security.addProvider(BouncyCastleProvider())
        val (id, keyPair, cert) = loadOrCreate()
        localId = id
        privateKey = keyPair.private
        publicKey = keyPair.public
        certificate = cert
    }

    /**
     * Loads existing TLS files or regenerates new keypair and certificate.
     *
     * @return Triple containing [UUID], [KeyPair], and self-signed [X509Certificate]
     */
    private fun loadOrCreate(): Triple<UUID, KeyPair, X509Certificate> {
        val certFile = securityLocalPath.resolve("node.crt")
        val keyFile = securityLocalPath.resolve("node.key")
        if (certFile.exists() && keyFile.exists()) {
            return try {
                val keyBytes = Files.readAllBytes(keyFile)
                val certBytes = Files.readAllBytes(certFile)

                val keyFactory = KeyFactory.getInstance("RSA", "BC")
                val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
                val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(certBytes))

                val cert = generateCertificate(KeyPair(publicKey, privateKey), "CN=local-node")

                Triple(UUID.randomUUID(), KeyPair(publicKey, privateKey), cert)
            } catch (e: Exception) {
                logger.warn("Failed to read existing TLS files: ${e.message}")
                regenerate()
            }
        } else {
            return regenerate()
        }
    }

    /**
     * Generates a new RSA key pair, a self-signed certificate, and stores them on disk.
     *
     * @return Triple containing [UUID], [KeyPair], and self-signed [X509Certificate]
     */
    private fun regenerate(): Triple<UUID, KeyPair, X509Certificate> {
        val parent = securityLocalPath
        parent.createDirectories()

        val localId = UUID.randomUUID()
        val keyGen = KeyPairGenerator.getInstance("RSA", "BC")
        keyGen.initialize(2048, SecureRandom())
        val keyPair = keyGen.generateKeyPair()

        val cert = generateCertificate(keyPair, "CN=local-node")

        val certFile = parent.resolve("node.crt")
        val keyFile = parent.resolve("node.key")

        certFile.writeBytes(cert.encoded)
        keyFile.writeBytes(keyPair.private.encoded)

        try {
            Files.getFileAttributeView(certFile, DosFileAttributeView::class.java)?.setHidden(true)
            Files.getFileAttributeView(keyFile, DosFileAttributeView::class.java)?.setHidden(true)
        } catch (_: Exception) {}

        logger.info(TranslationService.tr("cluster", "cluster.security.regenerated"))
        return Triple(localId, keyPair, cert)
    }

    /**
     * Generates a self-signed X.509 certificate for a given key pair and distinguished name.
     *
     * @param keyPair RSA key pair
     * @param dn Distinguished name (e.g., "CN=local-node")
     * @return Self-signed [X509Certificate]
     */
    private fun generateCertificate(keyPair: KeyPair, dn: String): X509Certificate {
        val now = Date()
        val expiry = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 year

        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            X500Name(dn),
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            expiry,
            X500Name(dn),
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.private)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer))
    }

    /**
     * Signs arbitrary data using the node's private key.
     *
     * @param data Byte array to sign
     * @return Base64-encoded signature string
     */
    fun sign(data: ByteArray): String {
        val signature = Signature.getInstance("SHA256withRSA", "BC")
        signature.initSign(privateKey)
        signature.update(data)
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    /**
     * Verifies a signature with a given public key.
     *
     * @param data Original byte array
     * @param signatureB64 Base64-encoded signature
     * @param pubKey Public key to verify with
     * @return true if signature is valid, false otherwise
     */
    fun verify(data: ByteArray, signatureB64: String, pubKey: PublicKey): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withRSA", "BC")
            signature.initVerify(pubKey)
            signature.update(data)
            signature.verify(Base64.getDecoder().decode(signatureB64))
        } catch (_: Exception) {
            false
        }
    }
}

/** Base64 encoding helpers for convenience */
fun PublicKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)
fun PrivateKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)