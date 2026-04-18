package de.polocloud.cli.communication.connection

import de.polocloud.cli.CliPaths
import de.polocloud.common.communication.certificate.CertificateStorage
import java.io.File
import java.nio.file.Path

/**
* Certificate storage for the **CLI** identity.
*
* Extends [CertificateStorage] — all key-pair generation, PEM I/O and
* registration-state logic is inherited. This class only declares file paths.
*
* Directory layout:
* ```
* <cli-cache>/identity/cli/
*   private-key.pem
*   public-key.pem
*   certificate.pem   ← signed by cluster CLI CA after registration
*   ca.pem            ← CLI CA cert received during registration
* ```
*
* Usage:
* ```kotlin
* val storage = CliCertificateStorage()
* storage.initialize()   // generates key pair if needed
*
* if (!storage.isRegistered()) {
    *     registrationClient.register(address, token)  // fills cert + ca
    * }
* // now open mTLS channel using storage.certificateFile() etc.
* ```
*/
class CliCertificateStorage : CertificateStorage() {

    private val identityDir: File = CliPaths.CACHE_DIR.resolve("identity/cli")

    override val storageDir: Path get() = identityDir.toPath()
    override fun certificateFile(): File = identityDir.resolve("certificate.pem")
    override fun privateKeyFile(): File = identityDir.resolve("private-key.pem")
    override fun publicKeyFile(): File = identityDir.resolve("public-key.pem")
    override fun caCertificateFile(): File = identityDir.resolve("ca.pem")
}
