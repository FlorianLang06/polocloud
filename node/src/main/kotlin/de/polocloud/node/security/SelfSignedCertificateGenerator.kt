package de.polocloud.node.security

import de.polocloud.common.generator.Generator
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

class SelfSignedCertificateGenerator(private val keyPair: KeyPair) : Generator<X509Certificate> {

    companion object {
        init {
            // Füge BouncyCastle Provider hinzu, falls noch nicht vorhanden
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    /**
     * Generates the self-signed certificate.
     */
    override fun generate(): X509Certificate {
        val now = Date()
        val until = Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 Jahr gültig

        val subject = X500Name("CN=Polocloud")  // Kann z. B. Node-ID sein
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        // Zertifikat bauen
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,           // issuer (selbstsigniert)
            serial,            // Serialnummer
            now,               // gültig ab
            until,             // gültig bis
            subject,           // subject
            keyPair.public     // öffentlicher Key
        )

        // Signierer
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.private)

        // Zertifikat erzeugen
        val certHolder = certBuilder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder)
    }
}