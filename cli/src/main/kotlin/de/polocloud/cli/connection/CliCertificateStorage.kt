package de.polocloud.cli.connection

import de.polocloud.cli.CliPaths
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security

/**
 * Stores and loads CLI certificates and keys for mTLS connections.
 *
 * Persists the following files in the CLI cache directory:
 * - private-key.pem: CLI client private key
 * - public-key.pem: CLI client public key
 * - certificate.pem: Signed client certificate from cluster CA
 * - ca.pem: Cluster CA certificate
 */
class CliCertificateStorage {

    private val storagePath = CliPaths.CACHE_DIR.resolve("identity")
    private val privateKeyFile = storagePath.resolve("private-key.pem")
    private val publicKeyFile = storagePath.resolve("public-key.pem")
    private val certificateFile = storagePath.resolve("certificate.pem")
    private val caCertificateFile = storagePath.resolve("ca.pem")

    val keyPair: KeyPair

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        if (!storagePath.exists()) {
            storagePath.mkdirs()
        }

        keyPair = loadOrCreateKeyPair()
    }

    /**
     * Checks if the CLI is registered (has valid certificates).
     */
    fun isRegistered(): Boolean =
        certificateFile.exists() && caCertificateFile.exists()

    /**
     * Saves the client certificate received from the cluster.
     *
     * @param certPem PEM-encoded certificate
     */
    fun saveCertificate(certPem: String) {
        certificateFile.writeText(certPem)
    }

    /**
     * Saves the CA certificate received from the cluster.
     *
     * @param caPem PEM-encoded CA certificate
     */
    fun saveCaCertificate(caPem: String) {
        caCertificateFile.writeText(caPem)
    }

    /**
     * Gets the client certificate file for mTLS setup.
     */
    fun certificateFile(): File = certificateFile

    /**
     * Gets the private key file for mTLS setup.
     */
    fun privateKeyFile(): File = privateKeyFile

    /**
     * Gets the CA certificate file for trust validation.
     */
    fun caCertificateFile(): File = caCertificateFile

    /**
     * Clears all stored certificates (for re-registration).
     */
    fun clearCertificates() {
        certificateFile.delete()
        caCertificateFile.delete()
    }

    private fun loadOrCreateKeyPair(): KeyPair {
        return if (privateKeyFile.exists() && publicKeyFile.exists()) {
            loadKeyPair()
        } else {
            generateAndSaveKeyPair()
        }
    }

    private fun generateAndSaveKeyPair(): KeyPair {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        writePem(privateKeyFile, kp.private)
        writePem(publicKeyFile, kp.public)
        return kp
    }

    /**
     * Loads a PEM-encoded RSA key pair via BouncyCastle.
     * Handles both PKCS#1 (traditional) and PKCS#8 encoded private keys.
     */
    private fun loadKeyPair(): KeyPair {
        val converter = JcaPEMKeyConverter().setProvider("BC")

        val privateKey = PEMParser(FileReader(privateKeyFile)).use { parser ->
            when (val obj = parser.readObject()) {
                is PEMKeyPair -> converter.getKeyPair(obj).private
                else -> converter.getPrivateKey(
                    org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(obj)
                )
            }
        }

        val publicKey = PEMParser(FileReader(publicKeyFile)).use { parser ->
            converter.getPublicKey(
                org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(parser.readObject())
            )
        }

        return KeyPair(publicKey, privateKey)
    }

    private fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }
}
