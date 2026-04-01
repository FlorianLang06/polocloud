package de.polocloud.node.security

import de.polocloud.node.common.rootDir
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileWriter
import java.math.BigInteger
import java.nio.file.Files
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

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

        // 🔥 Bootstrap Root of Trust
        if (!isRegistered()) {
            bootstrapRootOfTrust()
        }
    }

    fun isRegistered(): Boolean =
        certificateFile.exists() && caCertificateFile.exists()

    fun saveCertificate(certPem: String) {
        certificateFile.writeText(certPem)
    }

    fun saveCaCertificate(caPem: String) {
        caCertificateFile.writeText(caPem)
    }

    fun certificateFile(): File = certificateFile
    fun privateKeyFile(): File = privateKeyFile
    fun caCertificateFile(): File = caCertificateFile

    private fun bootstrapRootOfTrust() {
        // 1. CA KeyPair
        val caKeyPair = generateKeyPair()

        // 2. Self-signed CA Cert
        val caCert = generateCertificate(
            subject = "CN=Polocloud-Root-CA",
            keyPair = caKeyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA",
            isCa = true
        )

        // 3. Node Cert (signed by CA)
        val nodeCert = generateCertificate(
            subject = "CN=Node",
            keyPair = keyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA",
            isCa = false
        )

        writePem(caCertificateFile, caCert)
        writePem(certificateFile, nodeCert)
    }

    private fun generateCertificate(
        subject: String,
        keyPair: KeyPair,
        issuerKeyPair: KeyPair,
        issuer: String,
        isCa: Boolean
    ): X509Certificate {

        val now = Date()
        val until = Date(now.time + 3650L * 24 * 60 * 60 * 1000) // 10 Jahre

        val builder = JcaX509v3CertificateBuilder(
            X500Name(issuer),
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            until,
            X500Name(subject),
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .build(issuerKeyPair.private)

        val holder: X509CertificateHolder = builder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(holder)
    }

    private fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }

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
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
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