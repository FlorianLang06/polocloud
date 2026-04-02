package de.polocloud.cli.connection

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import kotlin.io.path.createDirectories

class CliCertificateStorage {

    private val storagePath = Path.of(".cache", "cli-security")
    private val privateKeyFile = storagePath.resolve("private-key.pem").toFile()
    private val publicKeyFile = storagePath.resolve("public-key.pem").toFile()
    private val certificateFile = storagePath.resolve("certificate.pem").toFile()
    private val caCertificateFile = storagePath.resolve("ca.pem").toFile()

    val keyPair: KeyPair

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        storagePath.createDirectories()
        keyPair = loadOrCreateKeyPair()
    }

    fun isRegistered(): Boolean = certificateFile.exists() && caCertificateFile.exists()

    fun saveCertificate(pem: String) { certificateFile.writeText(pem) }
    fun saveCaCertificate(pem: String) { caCertificateFile.writeText(pem) }

    fun certificateFile(): File = certificateFile
    fun privateKeyFile(): File = privateKeyFile
    fun caCertificateFile(): File = caCertificateFile

    private fun loadOrCreateKeyPair(): KeyPair {
        return if (privateKeyFile.exists() && publicKeyFile.exists()) {
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyFile.readBytes()))
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyFile.readBytes()))
            KeyPair(publicKey, privateKey)
        } else {
            val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            JcaPEMWriter(FileWriter(privateKeyFile)).use { it.writeObject(kp.private) }
            JcaPEMWriter(FileWriter(publicKeyFile)).use { it.writeObject(kp.public) }
            kp
        }
    }
}