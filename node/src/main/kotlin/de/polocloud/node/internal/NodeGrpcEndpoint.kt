package de.polocloud.node.internal

import de.polocloud.common.Address
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.cli.CliRegistrationService
import de.polocloud.node.cli.IpWhitelistInterceptor
import de.polocloud.node.configuration.ClusterConfiguration
import de.polocloud.node.security.CertificateDataStorage
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth

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
) {

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
            IpWhitelistInterceptor(clusterConfig.cliAccess)
        )
        .build()

    fun start() = server.start()
}
