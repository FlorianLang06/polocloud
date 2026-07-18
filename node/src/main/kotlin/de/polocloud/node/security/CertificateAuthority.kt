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
     * @param subjectAltNames optional SANs as GeneralNames
     */
    fun signCsr(
        csr: PKCS10CertificationRequest,
        validityDays: Int = 365,
        subjectAltNames: GeneralNames? = null
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
        if (subjectAltNames != null) {
            certBuilder.addExtension(
                org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,
                false,
                subjectAltNames
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

    /** Return the PEM encoded CA private key — only ever sent over an already-mTLS-secured
     *  channel to another verified node identity, see [de.polocloud.node.communication.impl.node.NodeServiceImpl.fetchClusterCa]. */
    fun getCaPrivateKeyPem(): String = pemEncode(caKeyPair.private)

    /** Return the PEM encoded CA public key, sent alongside [getCaPrivateKeyPem] so the
     *  receiving node can reconstruct a [KeyPair] without deriving the public key itself. */
    fun getCaPublicKeyPem(): String = pemEncode(caKeyPair.public)

    private fun pemEncode(key: java.security.Key): String {
        val writer = java.io.StringWriter()
        org.bouncycastle.openssl.jcajce.JcaPEMWriter(writer).use { it.writeObject(key) }
        return writer.toString()
    }
}