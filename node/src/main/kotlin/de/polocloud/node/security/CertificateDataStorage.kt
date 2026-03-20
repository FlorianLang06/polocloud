package de.polocloud.node.security

import de.polocloud.node.common.rootDir
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import org.bouncycastle.jce.provider.BouncyCastleProvider

class CertificateDataStorage {

    private val storagePath = rootDir().resolve(".cache")
    private val privateKeyFile = storagePath.resolve("private-key.pem").toFile()
    private val publicKeyFile = storagePath.resolve("public-key.pem").toFile()
    private val certificateFile = storagePath.resolve("certificate.pem").toFile()
    private val caCertificateFile = storagePath.resolve("ca.pem").toFile()

    val keyPair: KeyPair

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath)
        }

        keyPair = loadOrCreateKeyPair()
    }

    /** Prüft, ob Node bereits registriert ist */
    fun isRegistered(): Boolean = certificateFile.exists() && caCertificateFile.exists()

    /** Speichert das Zertifikat vom Cluster */
    fun saveCertificate(certPem: String) {
        certificateFile.writeText(certPem)
    }

    /** Speichert das CA-Zertifikat */
    fun saveCaCertificate(caPem: String) {
        caCertificateFile.writeText(caPem)
    }

    /** Gibt die Files für gRPC zurück */
    fun certificateFile(): File = certificateFile
    fun privateKeyFile(): File = privateKeyFile
    fun caCertificateFile(): File = caCertificateFile

    private fun loadOrCreateKeyPair(): KeyPair {
        return if (privateKeyFile.exists() && publicKeyFile.exists()) {
            loadKeyPair()
        } else {
            val keyPair = generateKeyPair()
            saveKeyPair(keyPair)
            keyPair
        }
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        return generator.generateKeyPair()
    }

    private fun saveKeyPair(keyPair: KeyPair) {
        JcaPEMWriter(FileWriter(privateKeyFile)).use { it.writeObject(keyPair.private) }
        JcaPEMWriter(FileWriter(publicKeyFile)).use { it.writeObject(keyPair.public) }
    }

    private fun loadKeyPair(): KeyPair {
        val privateSpec = PKCS8EncodedKeySpec(privateKeyFile.readBytes())
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(privateSpec)

        val publicSpec = X509EncodedKeySpec(publicKeyFile.readBytes())
        val publicKey: PublicKey = keyFactory.generatePublic(publicSpec)

        return KeyPair(publicKey, privateKey)
    }
}