package de.polocloud.node.registration

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.error.extensions.getOrReport
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.generator.CSPRNGGenerator
import de.polocloud.node.generator.CertificateSigningRequestGenerator
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.security.KeyPair
import java.util.UUID

class RegistrationManager(config: ClusterConfiguration, repository: NodeRepository, keyPair: KeyPair) : Closeable {

    private val logger = LoggerFactory.getLogger(RegistrationManager::class.java)

    val publicRegistrationToken = CSPRNGGenerator.generate()

    private var registrationServer = RegistrationServer(this,config.registration, repository, keyPair)

    fun allowRequests() {
        this.registrationServer.allowRequests()
    }

    fun tryJoinCluster(registrationInfo: RegistrationInfo, localId : UUID, certificateDataStorage : CertificateDataStorage) {
        val client = RegistrationClient()
        val csr = csrToPem(CertificateSigningRequestGenerator(certificateDataStorage.keyPair, localId).generate())

        val response = client.tryRegister(registrationInfo, localId, csr).getOrReport() ?: return

        if (!response.accepted) {
            logger.warn(TranslationService.tr("cluster", "cluster.registration.node.denied", "reason" to response.message))
            return
        }

        certificateDataStorage.saveCertificate(response.certificate)
        certificateDataStorage.saveCaCertificate(response.caCertificate)
    }

    fun csrToPem(csr: PKCS10CertificationRequest): String {
        val writer = StringWriter()
        JcaPEMWriter(writer).use {
            it.writeObject(csr)
        }
        return writer.toString()
    }

    override fun close(mode: ShutdownMode) {
        this.registrationServer.close(mode)
    }
}