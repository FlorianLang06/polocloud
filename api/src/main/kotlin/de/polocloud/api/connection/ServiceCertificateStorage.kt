package de.polocloud.api.connection

import de.polocloud.common.communication.certificate.CertificateStorage
import java.io.File
import java.nio.file.Path

/**
 * Certificate storage for a **standalone service / plugin** identity.
 *
 * Inherits all key-pair generation, PEM I/O and registration-state logic from
 * [CertificateStorage] and only declares where the PEM files live.
 *
 * The identity directory is provisioned by the node when it launches a service
 * (the signed certificate + CA are placed there), so the standalone API can open
 * an mTLS channel back to the node without performing its own registration.
 *
 * Directory layout:
 * ```
 * <identity-dir>/
 *   private-key.pem
 *   public-key.pem
 *   certificate.pem   ← signed by the cluster
 *   ca.pem            ← cluster CA certificate
 * ```
 */
class ServiceCertificateStorage(
    private val identityDir: File = defaultIdentityDir(),
) : CertificateStorage() {

    override val storageDir: Path get() = identityDir.toPath()
    override fun certificateFile(): File = identityDir.resolve("certificate.pem")
    override fun privateKeyFile(): File = identityDir.resolve("private-key.pem")
    override fun publicKeyFile(): File = identityDir.resolve("public-key.pem")
    override fun caCertificateFile(): File = identityDir.resolve("ca.pem")

    companion object {

        /**
         * Resolves the identity directory from the `polocloud.identity.dir` system
         * property, the `POLOCLOUD_IDENTITY_DIR` environment variable, or defaults
         * to `identity/service` relative to the working directory.
         */
        fun defaultIdentityDir(): File {
            val configured = System.getProperty("polocloud.identity.dir")
                ?: System.getenv("POLOCLOUD_IDENTITY_DIR")
            return File(configured ?: "identity/service")
        }
    }
}
