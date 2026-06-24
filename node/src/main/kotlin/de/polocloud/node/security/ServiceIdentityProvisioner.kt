package de.polocloud.node.security

import de.polocloud.common.communication.certificate.certToPem
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator

/**
 * Provisions the mTLS identity a locally launched service needs to talk back to
 * the node via the standalone API.
 *
 * The node owns the CA, so instead of having the service perform a CSR handshake
 * it directly generates a key pair, signs a certificate with the node CA and lays
 * the PEM files out in the directory layout the API's
 * [de.polocloud.api.connection.ServiceCertificateStorage] expects:
 *
 * ```
 * <identity-dir>/
 *   private-key.pem
 *   public-key.pem
 *   certificate.pem   ← signed by the cluster CA
 *   ca.pem            ← cluster CA certificate
 * ```
 *
 * The identity directory is then handed to the process via the
 * `POLOCLOUD_IDENTITY_DIR` environment variable so the API picks it up on its
 * first call.
 */
object ServiceIdentityProvisioner {

    /**
     * Generates and writes a freshly signed identity for the given service into
     * [identityDir]. Any previously provisioned files are overwritten so a
     * restarted service always receives a valid certificate.
     *
     * @param identityDir target directory for the PEM files (created if missing)
     * @param serviceId   unique id of the service instance (used as cert CN + SAN)
     * @param planName    group / plan name the service belongs to (added as SAN)
     */
    fun provision(identityDir: File, serviceId: String, planName: String) {
        identityDir.mkdirs()

        val keyPair = generateKeyPair()
        val ca = NodeCertificateStorage.certificateAuthority()
        val signed = ca.signCsr(
            buildCsr(keyPair, serviceId),
            subjectAltNames = SanBuilder.forService(serviceId, planName),
        )

        writePem(File(identityDir, "private-key.pem"), keyPair.private)
        writePem(File(identityDir, "public-key.pem"), keyPair.public)
        File(identityDir, "certificate.pem").writeText(certToPem(signed))
        File(identityDir, "ca.pem").writeText(ca.getCaCertificatePem())
    }

    private fun buildCsr(keyPair: KeyPair, serviceId: String): PKCS10CertificationRequest {
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        return JcaPKCS10CertificationRequestBuilder(X500Name("CN=$serviceId"), keyPair.public)
            .build(signer)
    }

    private fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()

    private fun writePem(file: File, obj: Any) {
        JcaPEMWriter(FileWriter(file)).use { it.writeObject(obj) }
    }
}