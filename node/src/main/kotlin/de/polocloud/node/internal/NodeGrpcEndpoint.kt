package de.polocloud.node.internal

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.cli.registration.CliRegistrationService
import de.polocloud.node.cli.interceptor.CliSessionInterceptor
import de.polocloud.node.cli.interceptor.IpWhitelistInterceptor
import de.polocloud.node.cli.session.CliSessionCleanup
import de.polocloud.node.cli.session.ICliSessionManager
import de.polocloud.node.node.NodeServiceImpl
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.nodes.LocalNodeContainer
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
    clusterConfig: ClusterConfiguration,
    cliRegistrationService: CliRegistrationService,
    cliSessionManager: ICliSessionManager,
    localNodeContainerProvider: () -> LocalNodeContainer
) : Closeable {

    private val nodeService = NodeServiceImpl(localNodeContainerProvider)
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
            IpWhitelistInterceptor(clusterConfig.cliAccess),
            CliSessionInterceptor(cliSessionManager)
        )
        .interceptedService(
            nodeService,
            IpWhitelistInterceptor(clusterConfig.cliAccess),
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
