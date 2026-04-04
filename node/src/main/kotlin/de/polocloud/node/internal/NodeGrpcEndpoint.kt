package de.polocloud.node.internal

import de.polocloud.common.Address
import de.polocloud.common.error.extensions.report
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.cli.CliRegistrationService
import de.polocloud.node.cli.CliSessionManager
import de.polocloud.node.cli.interceptor.CliSessionInterceptor
import de.polocloud.node.cli.interceptor.IpWhitelistInterceptor
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.error.NodeError
import de.polocloud.node.security.CertificateDataStorage
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * gRPC endpoint for the node/cluster.
 *
 * Hosts all services on a single port with mTLS (ClientAuth.REQUIRE):
 * - Node registration + cluster services (trusted via node CA)
 * - CLI registration (trusted via CLI CA, additionally IP-whitelisted)
 *
 * Both the node CA and the CLI CA are passed as trust anchors so the server
 * accepts both node peers and CLI clients on the same TLS listener.
 */
class NodeGrpcEndpoint(
    address: Address,
    certificateDataStorage: CertificateDataStorage,
    clusterConfig: ClusterConfiguration,
    cliRegistrationService: CliRegistrationService,
    private val cliSessionManager: CliSessionManager,
) {

    private val logger = LoggerFactory.getLogger(NodeGrpcEndpoint::class.java)
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            certFile = certificateDataStorage.certificateFile(),
            keyFile = certificateDataStorage.privateKeyFile(),
            clientAuth = ClientAuth.REQUIRE,
            caCertFiles = arrayOf(
                certificateDataStorage.caCertificateFile(),
                certificateDataStorage.cliCaCertificateFile()
            )
        )
        .interceptedService(
            cliRegistrationService,
            IpWhitelistInterceptor(clusterConfig.cliAccess),
            CliSessionInterceptor(cliSessionManager)
        )
        .build()

    fun start() {
        server.start()
        startSessionCleanup()
    }

    private fun startSessionCleanup() {
        val timeout = 60_000L

        cleanupExecutor.scheduleAtFixedRate(
            {
                runCatching {
                    val before = cliSessionManager.all().size

                    cliSessionManager.cleanupExpired(timeout)

                    val after = cliSessionManager.all().size
                    val removed = before - after

                    if (removed > 0) {
                        logger.debug("Cleaned up $removed expired CLI sessions")
                    }
                }.onFailure { ex ->
                    NodeError.SessionCleanupFailed(
                        reason = ex.message ?: "unknown"
                    ).report()
                }
            },
            0,
            timeout,
            TimeUnit.MILLISECONDS
        )
    }
}
