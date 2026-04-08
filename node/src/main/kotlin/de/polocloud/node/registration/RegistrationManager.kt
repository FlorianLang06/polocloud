package de.polocloud.node.registration

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.error.extensions.getOrReport
import de.polocloud.common.generator.CertificateSigningRequestGenerator
import de.polocloud.common.security.toPem
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.cli.registration.CliRegistrationService
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.registration.token.RegistrationTokenManager
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage
import org.slf4j.LoggerFactory
import java.util.UUID

class RegistrationManager(
    config: ClusterConfiguration,
    repository: NodeRepository,
    cliRegistrationService: CliRegistrationService,
) : Closeable {

    private val logger = LoggerFactory.getLogger(RegistrationManager::class.java)

    val registrationTokenManger = RegistrationTokenManager()

    private val registrationServer = RegistrationServer(
        registrationManager = this,
        address = config.registration,
        nodeRepository = repository,
        clusterConfig = config,
        cliRegistrationService = cliRegistrationService,
    )

    fun allowRequests() = registrationServer.allowRequests()

    fun tryJoinCluster(registrationInfo: RegistrationInfo, localId : UUID) {
        val client = RegistrationClient()
        val csr = CertificateSigningRequestGenerator(CertificateDataStorage.keyPair, localId).generate().toPem()

        val response = client.tryRegister(registrationInfo, localId, csr).getOrReport() ?: return

        if (!response.accepted) {
            logger.warn(TranslationService.tr("cluster", "cluster.registration.node.denied", "reason" to response.message))
            return
        }
        CertificateDataStorage.saveCertificates(response.certificate,response.caCertificate)
    }

    override fun close(mode: ShutdownMode) = registrationServer.close(mode)
}