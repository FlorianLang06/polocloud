package de.polocloud.node.security

import de.polocloud.node.generator.SelfSignedCertificateGenerator
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator

class SecurityCertificateData(path: Path) {

    private val certFile = path.resolve("certificate.pem").toFile()
    private val keyFile = path.resolve("private-key.pem").toFile()

    init {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }

        if (!certFile.exists() || !keyFile.exists()) {
            generateCertificate()
        }
    }

    fun certificateFile(): File = certFile
    fun keyFile(): File = keyFile

    private fun generateCertificate() {
        val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()

        val cert = SelfSignedCertificateGenerator(keyPair).generate()

        // Write certificate and key in PEM format (ASCII)
        JcaPEMWriter(FileWriter(certFile)).use { it.writeObject(cert) }
        JcaPEMWriter(FileWriter(keyFile)).use { it.writeObject(keyPair.private) }
    }
}