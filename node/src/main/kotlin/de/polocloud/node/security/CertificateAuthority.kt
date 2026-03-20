package de.polocloud.node.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*

/**
 * Manages a CA keypair and signs incoming CSRs.
 */
class CertificateAuthority(
    val caKeyPair: KeyPair,
    val caCertificate: X509Certificate
) {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Signs a PKCS10 CSR and returns a signed X509Certificate.
     * @param csr the CSR from the node
     * @param validityDays how long the certificate is valid
     * @param subjectAltNames optional SANs (IP or DNS)
     */
    fun signCsr(
        csr: PKCS10CertificationRequest,
        validityDays: Int = 365,
        subjectAltNames: List<String> = emptyList()
    ): X509Certificate {

        val now = Date()
        val until = Date(now.time + validityDays.toLong() * 24 * 60 * 60 * 1000)

        val csrSubject = csr.subject
        val csrPublicKey = org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
            .setProvider("BC")
            .getPublicKey(csr.subjectPublicKeyInfo)

        // serial number sicher erzeugen
        val serial = BigInteger(64, SecureRandom())

        val certBuilder = JcaX509v3CertificateBuilder(
            X500Name(caCertificate.subjectX500Principal.name), // issuer = CA
            serial,
            now,
            until,
            csrSubject, // subject = Node
            csrPublicKey
        )

        // SAN hinzufügen, falls vorhanden
        if (subjectAltNames.isNotEmpty()) {
            val sanArray = subjectAltNames.map { name ->
                if (name.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    GeneralName(GeneralName.iPAddress, name)
                } else {
                    GeneralName(GeneralName.dNSName, name)
                }
            }.toTypedArray()
            certBuilder.addExtension(
                org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,
                false,
                GeneralNames(sanArray)
            )
        }

        // Signieren
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(caKeyPair.private)

        val certHolder = certBuilder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder)
    }

    /** Return the PEM encoded CA certificate */
    fun getCaCertificatePem(): String {
        val writer = java.io.StringWriter()
        org.bouncycastle.openssl.jcajce.JcaPEMWriter(writer).use { it.writeObject(caCertificate) }
        return writer.toString()
    }
}