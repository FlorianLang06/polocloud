package de.polocloud.node.generator

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*

/**
 * Generates a self-signed X.509 certificate using a provided KeyPair.
 *
 * <p>This ensures that the generated certificate matches the given private key,
 * which is required for TLS/mTLS usage.</p>
 *
 * <p>The subject and issuer are identical (self-signed).</p>
 */
class SelfSignedCertificateGenerator(val keyPair: KeyPair) : Generator<X509Certificate> {

    companion object {
        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    /**
     * Generates a self-signed certificate for the given KeyPair.
     *
     * @param keyPair the key pair to use (must be preserved for TLS)
     * @return generated X509Certificate
     */
    override fun generate(): X509Certificate {

        val now = Date()
        val until = Date(now.time + 365L * 24 * 60 * 60 * 1000)

        val subject = X500Name("CN=Polocloud")
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            now,
            until,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certHolder)
    }
}