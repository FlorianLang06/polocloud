package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.communication.cli.session.CliSessionCleanup
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.impl.cluster.ClusterServiceImpl
import de.polocloud.node.communication.impl.node.NodeServiceImpl
import de.polocloud.node.communication.impl.services.ServiceManagerImpl
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.communication.interceptor.IpWhitelistInterceptor
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.security.NodeCertificateStorage

/**
 * gRPC endpoint for the cluster node.
 *
 * Hosts all services on a single mTLS port ([de.polocloud.common.communication.tls.ClientAuthMode.REQUIRE]):
 * - CLI registration and commands  → trusted via the shared CA, additionally IP-whitelisted
 * - Node-to-node communication     → trusted via the same CA
 *
 * Both client types share one CA, so a single [MtlsConfig.mutual] call is sufficient.
 * If separate CAs are introduced later, switch to [MtlsConfig.mutual] with multiple files.
 *
 * Session cleanup is owned here because this class controls the server lifecycle —
 * cleanup starts when the server starts and stops when the server stops.
 */
class NodeGrpcEndpoint(
    address: Address,
    cliRegistrationService: CliRegistrationService,
    cliSessionManager: ICliSessionManager
) : Closeable {

    private val executor = GrpcModule.createExecutor()

    private val nodeService = NodeServiceImpl(executor)
    private val clusterService = ClusterServiceImpl(executor)
    private val serviceManager = ServiceManagerImpl(executor)

    private val sessionCleanup = CliSessionCleanup(cliSessionManager)

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            MtlsConfig.mutual(
                cert = NodeCertificateStorage.certificateFile(),
                key = NodeCertificateStorage.privateKeyFile(),
                caCert = NodeCertificateStorage.caCertificateFile(),
            )
        )
        .interceptedService(
            cliRegistrationService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            nodeService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            clusterService,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            serviceManager,
            IpWhitelistInterceptor(),
            CliSessionInterceptor(cliSessionManager)
        )
        .build()

    fun start() {
        server.start()
        sessionCleanup.start()
    }

    override fun close(mode: ShutdownMode) {
        nodeService.broadcastShutdown()
        sessionCleanup.close(mode)
        server.close(mode)
    }
}
