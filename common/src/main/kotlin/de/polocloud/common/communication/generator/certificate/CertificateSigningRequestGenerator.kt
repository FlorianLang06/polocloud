package de.polocloud.common.communication.generator.certificate

import de.polocloud.common.communication.generator.Generator
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.security.KeyPair
import java.security.Security
import java.util.UUID

class CertificateSigningRequestGenerator(
    private val keyPair: KeyPair,
    private val localId: UUID
) : Generator<PKCS10CertificationRequest> {

    companion object {
        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    override fun generate(): PKCS10CertificationRequest {
        val subject = X500Name("CN=${localId}")

        val builder = JcaPKCS10CertificationRequestBuilder(
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)

        return builder.build(signer)
    }
}