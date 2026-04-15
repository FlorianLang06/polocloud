package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.node.communication.cli.session.CliSessionCleanup
import de.polocloud.node.communication.cli.session.ICliSessionManager
import de.polocloud.node.communication.impl.cluster.ClusterServiceImpl
import de.polocloud.node.communication.impl.node.NodeServiceImpl
import de.polocloud.node.communication.impl.services.ServiceManagerImpl
import de.polocloud.node.communication.interceptor.CliSessionInterceptor
import de.polocloud.node.communication.interceptor.IpWhitelistInterceptor
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.security.CertificateDataStorage
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth

/**
 * gRPC endpoint for the cluster node.
 *
 * Hosts all services on a single mTLS port (ClientAuth.REQUIRE):
 * - CLI registration and commands  → trusted via CLI CA, additionally IP-whitelisted
 * - Node-to-node communication     → trusted via node CA
 *
 * Both CAs are passed as trust anchors so the server accepts both client types
 * on the same TLS listener.
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
            certFile = CertificateDataStorage.certificateFile(),
            keyFile = CertificateDataStorage.privateKeyFile(),
            clientAuth = ClientAuth.REQUIRE,
            caCertFiles = arrayOf(
                CertificateDataStorage.caCertificateFile()
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
