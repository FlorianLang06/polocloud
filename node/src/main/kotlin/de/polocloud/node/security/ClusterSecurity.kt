package de.polocloud.node.security

import de.polocloud.i18n.api.TranslationService
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import kotlin.io.path.createDirectories
import java.util.Base64
import java.util.Date
import java.math.BigInteger
import java.io.File
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Manages cluster security for a local node with TLS support.
 *
 * Responsibilities:
 * - Generates or loads a persistent RSA key pair.
 * - Creates a self-signed X.509 certificate for TLS usage.
 * - Stores private key and certificate as PEM files for gRPC TLS.
 * - Signs and verifies arbitrary data for cluster authentication.
 *
 * Security:
 * - PrivateKey remains only on the local node.
 * - PublicKey and Certificate can be shared with other nodes or clients.
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

    private fun loadOrCreate(): Triple<UUID, KeyPair, X509Certificate> {
        val certFile = securityLocalPath.resolve("node.crt").toFile()
        val keyFile = securityLocalPath.resolve("node.key").toFile()

        return if (certFile.exists() && keyFile.exists()) {
            try {
                val keyFactory = KeyFactory.getInstance("RSA", "BC")
                val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Files.readAllBytes(keyFile.toPath())))

                val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
                val certificate = certFactory.generateCertificate(certFile.inputStream()) as X509Certificate

                val publicKey = certificate.publicKey
                val keyPair = KeyPair(publicKey, privateKey)

                Triple(UUID.randomUUID(), keyPair, certificate)
            } catch (e: Exception) {
                logger.warn("Failed to read existing TLS files: ${e.message}")
                regenerate()
            }
        } else {
            regenerate()
        }
    }

    private fun regenerate(): Triple<UUID, KeyPair, X509Certificate> {
        val parent = securityLocalPath
        parent.createDirectories()

        val localId = UUID.randomUUID()

        val keyGen = KeyPairGenerator.getInstance("RSA", "BC")
        keyGen.initialize(2048, SecureRandom())
        val keyPair = keyGen.generateKeyPair()

        val cert = generateCertificate(keyPair, "CN=local-node")

        writeCertificatePem(cert, parent.resolve("node.crt").toFile())
        writePrivateKeyPem(keyPair.private, parent.resolve("node.key").toFile())

        logger.info(TranslationService.tr("cluster", "cluster.security.regenerated"))
        return Triple(localId, keyPair, cert)
    }

    private fun writeCertificatePem(cert: X509Certificate, file: File) {
        val pem = "-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(cert.encoded) +
                "\n-----END CERTIFICATE-----\n"
        file.writeText(pem)
    }

    private fun writePrivateKeyPem(privateKey: PrivateKey, file: File) {
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(privateKey.encoded) +
                "\n-----END PRIVATE KEY-----\n"
        file.writeText(pem)
    }

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

    private fun publicKeyFromCert(certBytes: ByteArray): PublicKey {
        val cf = java.security.cert.CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(certBytes.inputStream())
        return cert.publicKey
    }

    /** Signs arbitrary data using the node's private key */
    fun sign(data: ByteArray): String {
        val signature = Signature.getInstance("SHA256withRSA", "BC")
        signature.initSign(privateKey)
        signature.update(data)
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    /** Verifies a signature with a given public key */
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

    fun certFile() = securityLocalPath.resolve("node.crt").toFile()
    fun keyFile() = securityLocalPath.resolve("node.key").toFile()
}

/** Base64 encoding helpers */
fun PublicKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)
fun PrivateKey.toBase64(): String = Base64.getEncoder().encodeToString(this.encoded)