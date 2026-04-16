package de.polocloud.node.security

import de.polocloud.node.utils.rootDir
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
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
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*

object CertificateDataStorage {

    private val basePath = rootDir().resolve(".cache/identity")
    private val nodePath = basePath.resolve("node")
    private val caPath = basePath.resolve("ca")

    private val privateKeyFile = nodePath.resolve("private-key.pem").toFile()
    private val publicKeyFile = nodePath.resolve("public-key.pem").toFile()

    private val caPrivateKeyFile = caPath.resolve("private-key.pem").toFile()
    private val caPublicKeyFile = caPath.resolve("public-key.pem").toFile()

    private val certificateFile = nodePath.resolve("certificate.pem").toFile()
    private val caCertificateFile = caPath.resolve("certificate.pem").toFile()

    lateinit var keyPair: KeyPair
    lateinit var caKeyPair: KeyPair

    lateinit var nodeId: String

    fun initialize() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        Files.createDirectories(nodePath)
        Files.createDirectories(caPath)

        keyPair = loadOrCreateKeyPair(privateKeyFile, publicKeyFile)
        caKeyPair = loadOrCreateKeyPair(caPrivateKeyFile, caPublicKeyFile)

        if (!isInitialized()) {
            bootstrap()
        }
    }

    fun isInitialized(): Boolean =
        certificateFile.exists() && caCertificateFile.exists()

    fun certificateFile(): File = certificateFile
    fun privateKeyFile(): File = privateKeyFile
    fun caCertificateFile(): File = caCertificateFile

    fun saveCertificates(certPem: String, caPem: String) {
        certificateFile.writeText(certPem)
        caCertificateFile.writeText(caPem)
    }

    fun loadCaCertificate(): X509Certificate {
        return loadCertificateFromPem(caCertificateFile)
    }

    fun certificateAuthority(): CertificateAuthority {
        return CertificateAuthority(
            caKeyPair,
            loadCaCertificate()
        )
    }

    private fun bootstrap() {
        val caCert = generateCertificate(
            subject = "CN=Polocloud-Root-CA",
            keyPair = caKeyPair,
            issuerKeyPair = caKeyPair,
            issuer = "CN=Polocloud-Root-CA",
            isCa = true
        )

        val nodeCert = generateCertificate(
            subject = "CN=Polocloud-Node",
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
        val until = Date(now.time + 3650L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            X500Name(issuer),
            BigInteger(64, SecureRandom()),
            now,
            until,
            X500Name(subject),
            keyPair.public
        )

        if (isCa) {
            builder.addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true)
            )

            builder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(
                    KeyUsage.keyCertSign or
                            KeyUsage.cRLSign
                )
            )
        } else {
            builder.addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(false)
            )

            builder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(
                    KeyUsage.digitalSignature or
                            KeyUsage.keyEncipherment
                )
            )

            val spec = NodeIdentityPolicy.resolve(nodeId)

            builder.addExtension(
                Extension.subjectAlternativeName,
                false,
                SanBuilder.build(spec)
            )
        }

        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .build(issuerKeyPair.private)

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

    private fun loadOrCreateKeyPair(private: File, pub: File): KeyPair {
        return if (private.exists() && pub.exists()) {
            loadKeyPairFromFiles(private, pub)
        } else {
            val kp = generateKeyPair()
            writeKeyPair(kp, private, pub)
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
                SubjectPublicKeyInfo.getInstance(parser.readObject())
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