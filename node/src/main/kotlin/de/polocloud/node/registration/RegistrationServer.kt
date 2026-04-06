package de.polocloud.node.registration

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.cli.registration.CliRegistrationService
import de.polocloud.node.cli.interceptor.IpWhitelistInterceptor
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage

class RegistrationServer(
    registrationManager: RegistrationManager,
    address: Address,
    nodeRepository: NodeRepository,
    certificateDataStorage : CertificateDataStorage,
    clusterConfig: ClusterConfiguration,
    cliRegistrationService: CliRegistrationService,
) : Closeable {

    private val grpcServer = GrpcEndpoint.Builder(address)
        .service(RegistrationService(registrationManager, nodeRepository, certificateDataStorage))
        .interceptedService(
            cliRegistrationService,
            IpWhitelistInterceptor(clusterConfig.cliAccess)
        )
        .build()

    fun allowRequests() {
        this.grpcServer.start()
    }

    override fun close(mode: ShutdownMode) {
        // maybe a possible reason for a not clean close
        this.grpcServer.close(ShutdownMode.GRACEFUL)
    }
}