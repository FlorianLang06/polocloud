package de.polocloud.node.registration

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.cli.CliRegistrationService
import de.polocloud.node.cli.IpWhitelistInterceptor
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.repositories.NodeRepository
import java.security.KeyPair

class RegistrationServer(
    registrationManager: RegistrationManager,
    address: Address,
    nodeRepository: NodeRepository,
    keyPair : KeyPair,
    clusterConfig: ClusterConfiguration,
    cliRegistrationService: CliRegistrationService,
) : Closeable {

    private val grpcServer = GrpcEndpoint.Builder(address)
        .service(RegistrationService(registrationManager, nodeRepository, keyPair))
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