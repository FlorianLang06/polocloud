package de.polocloud.node.security

import de.polocloud.node.common.rootDir
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.math.BigInteger
import java.nio.file.Files
import java.security.*
import java.security.cert.X509Certificate
import java.util.*

class CertificateDataStorage {

    private val basePath = rootDir().resolve(".cache/identity/local-node")
    private val clusterPath = basePath.resolve("cluster")
    private val cliPath = basePath.resolve("cli")

    private val privateKeyFile = clusterPath.resolve("private-key.pem").toFile()
    private val publicKeyFile = clusterPath.resolve("public-key.pem").toFile()
    private val certificateFile = clusterPath.resolve("certificate.pem").toFile()
    private val caCertificateFile = clusterPath.resolve("ca.pem").toFile()

    private val cliCaCertificateFile = cliPath.resolve("ca.pem").toFile()
    private val cliCaPrivateKeyFile = cliPath.resolve("ca-private-key.pem").toFile()
    private val cliCaPublicKeyFile = cliPath.resolve("ca-public-key.pem").toFile()

    val keyPair: KeyPair

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        Files.createDirectories(clusterPath)
        Files.createDirectories(cliPath)

        keyPair = loadOrCreateKeyPair()

        // 🔥 Bootstrap Root of Trust
        if (!isNodeRegistered()) {
            bootstrapNodeRootOfTrust()
        }

        if (!isCliCaPresent()) {
            bootstrapCliCa()
        }
    }

    fun isNodeRegistered(): Boolean =
        certificateFile.exists() && caCertificateFile.exists()

    fun isCliCaPresent(): Boolean = cliCaCertificateFile.exists()

    fun saveCertificate(certPem: String) {
        certificateFile.writeText(certPem)
    }

    fun saveCaCertificate(caPem: String) {
        caCertificateFile.writeText(caPem)
    }

    fun certificateFile(): File = certificateFile
    fun privateKeyFile(): File = privateKeyFile
    fun caCertificateFile(): File = caCertificateFile
    fun cliCaCertificateFile(): File = cliCaCertificateFile

    fun loadCliCertificateAuthority(): CertificateAuthority {
        val caKeyPair = if (cliCaPrivateKeyFile.exists() && cliCaPublicKeyFile.exists()) {
            loadKeyPairFromFiles(cliCaPrivateKeyFile, cliCaPublicKeyFile)
        } else {
            val kp = generateKeyPair()
            writeKeyPair(kp, cliCaPrivateKeyFile, cliCaPublicKeyFile)
            kp
        }

        val caCert = loadCertificateFromPem(cliCaCertificateFile)
        return CertificateAuthority(caKeyPair, caCert)
    }

    private fun bootstrapNodeRootOfTrust() {
        val caKeyPair = generateKeyPair()

        val caCert = generateCertificate(
            subject = "CN=Polocloud-Root-CA",
            keyPair = caKeyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA"
        )

        val nodeCert = generateCertificate(
            subject = "CN=Node",
            keyPair = keyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA"
        )

        writePem(caCertificateFile, caCert)
        writePem(certificateFile, nodeCert)
    }

    private fun bootstrapCliCa() {
        val caKeyPair = generateKeyPair()
        writeKeyPair(caKeyPair, cliCaPrivateKeyFile, cliCaPublicKeyFile)

        val caCert = generateCertificate(
            subject       = "CN=Polocloud-CLI-CA",
            keyPair       = caKeyPair,
            issuerKeyPair = caKeyPair,
            issuer        = "CN=Polocloud-CLI-CA"
        )

        writePem(cliCaCertificateFile, caCert)
    }

    private fun generateCertificate(
        subject: String,
        keyPair: KeyPair,
        issuerKeyPair: KeyPair,
        issuer: String
    ): X509Certificate {
        val now   = Date()
        val until = Date(now.time + 3650L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            X500Name(issuer),
            BigInteger.valueOf(System.currentTimeMillis()),
            now,
            until,
            X500Name(subject),
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(issuerKeyPair.private)
        val holder: X509CertificateHolder = builder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(holder)
    }

    private fun loadCertificateFromPem(file: File): X509Certificate {
        PEMParser(FileReader(file)).use { parser ->
            val obj = parser.readObject()
            return JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(obj as X509CertificateHolder)
        }
    }

    private fun loadOrCreateKeyPair(): KeyPair {
        return if (privateKeyFile.exists() && publicKeyFile.exists()) {
            loadKeyPairFromFiles(privateKeyFile, publicKeyFile)
        } else {
            val kp = generateKeyPair()
            writeKeyPair(kp, privateKeyFile, publicKeyFile)
            kp
        }
    }

    private fun loadKeyPairFromFiles(privateFile: File, publicFile: File): KeyPair {
        val privateKey = PEMParser(FileReader(privateFile)).use { parser ->
            when (val obj = parser.readObject()) {
                is PEMKeyPair -> JcaPEMKeyConverter().setProvider("BC").getKeyPair(obj).private
                else -> JcaPEMKeyConverter().setProvider("BC").getPrivateKey(
                    org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(obj)
                )
            }
        }

        val publicKey = PEMParser(FileReader(publicFile)).use { parser ->
            JcaPEMKeyConverter().setProvider("BC").getPublicKey(
                org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(parser.readObject())
            )
        }

        return KeyPair(publicKey, privateKey)
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    private fun writeKeyPair(keyPair: KeyPair, privateFile: File, publicFile: File) {
        writePem(privateFile, keyPair.private)
        writePem(publicFile, keyPair.public)
    }

    private fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }
}