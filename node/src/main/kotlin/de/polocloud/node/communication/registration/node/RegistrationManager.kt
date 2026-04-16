package de.polocloud.node.communication.registration.node

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationHolder
import de.polocloud.common.communication.generator.certificate.CertificateSigningRequestGenerator
import de.polocloud.common.communication.security.toPem
import de.polocloud.i18n.api.TranslationService
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.registration.client.RegistrationClient
import de.polocloud.node.communication.registration.node.token.RegistrationTokenManager
import de.polocloud.node.communication.registration.server.RegistrationServer
import de.polocloud.node.core.configuration.NodeConfigurations
import de.polocloud.node.security.CertificateDataStorage
import org.slf4j.LoggerFactory
import java.util.*

class RegistrationManager(
    val holder: ConfigurationHolder<NodeConfigurations>,
    cliRegistrationService: CliRegistrationService,
) : Closeable {

    private val logger = LoggerFactory.getLogger(RegistrationManager::class.java)

    val registrationTokenManger = RegistrationTokenManager()

    private val registrationServer = RegistrationServer(
        registrationManager = this,
        address = holder.value.cluster.registration,
        cliRegistrationService = cliRegistrationService,
    )

    fun allowRequests() = registrationServer.allowRequests()

    fun tryJoinCluster(registrationInfo: RegistrationInfo, localId : UUID, group: String) {
        val client = RegistrationClient()
        val csr = CertificateSigningRequestGenerator(CertificateDataStorage.keyPair, localId).generate().toPem()

        val response = client.tryRegister(registrationInfo.address,registrationInfo.token, holder.value.general.hostname, holder.value.general.bindAddress.port, localId, group, csr)

        if (!response.accepted) {
            logger.warn(TranslationService.tr("cluster", "cluster.registration.node.denied", "reason" to response.message))
            return
        }
        CertificateDataStorage.saveCertificates(response.certificate,response.caCertificate)
    }

    override fun close(mode: ShutdownMode) = registrationServer.close(mode)
}