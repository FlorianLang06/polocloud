package de.polocloud.services.sdk.communication.security

import de.polocloud.common.communication.certificate.CertificateStorage
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Certificate storage for a **service instance** identity.
 *
 * Files are stored in a subdirectory of the service's working directory
 * (injected via the `service.identity.dir` system property set by
 * [de.polocloud.node.services.ServiceFactory]):
 *
 * ```
 * <working-dir>/.identity/
 *   private-key.pem
 *   public-key.pem
 *   certificate.pem    ← written by ServiceRegistrationClient after handshake
 *   ca.pem             ← written by ServiceRegistrationClient after handshake
 * ```
 *
 * The key pair is generated once on the first start and reused on restarts.
 * Certificates are obtained via the registration handshake with the node and
 * overwritten every time the service boots (short-lived certs).
 */
class ServiceCertificateStorage(identityDir: Path) : CertificateStorage() {

    override val storageDir: Path = identityDir

    override fun certificateFile(): File = storageDir.resolve("certificate.pem").toFile()
    override fun privateKeyFile(): File = storageDir.resolve("private-key.pem").toFile()
    override fun publicKeyFile(): File = storageDir.resolve("public-key.pem").toFile()
    override fun caCertificateFile(): File = storageDir.resolve("ca.pem").toFile()

    companion object {

        /**
         * Creates a [ServiceCertificateStorage] whose directory is read from
         * the `service.identity.dir` system property.
         *
         * Called by [de.polocloud.services.sdk.ServiceBoot] during startup.
         */
        fun fromSystemProperties(): ServiceCertificateStorage {
            val dir = System.getProperty("service.identity.dir")
                ?: error("Required system property 'service.identity.dir' is not set")
            return ServiceCertificateStorage(Paths.get(dir))
        }
    }
}