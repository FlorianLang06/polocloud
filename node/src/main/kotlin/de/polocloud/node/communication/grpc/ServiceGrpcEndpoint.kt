package de.polocloud.node.communication.grpc

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.common.communication.tls.MtlsConfig
import de.polocloud.node.communication.impl.group.GroupApiServiceImpl
import de.polocloud.node.group.GroupService
import de.polocloud.node.security.NodeCertificateStorage

/**
 * Dedicated mTLS gRPC endpoint for **services and plugins** that use the
 * standalone Polocloud API.
 *
 * Intentionally kept separate from [NodeGrpcEndpoint] — which serves CLI and
 * node-to-node traffic — so plugin API calls never share a port or the
 * CLI-specific interceptor chain (IP whitelist, CLI sessions) with the cluster
 * control plane.
 *
 * Trust is anchored on the same node CA, so the per-service identity the node
 * provisions when it launches a service (certificate + CA via
 * [de.polocloud.node.security.ServiceIdentityProvisioner]) is accepted here.
 */
class ServiceGrpcEndpoint(
    address: Address,
    groupService: GroupService,
) : Closeable {

    private val executor = GrpcModule.createExecutor(groupService)
    private val groupApiService = GroupApiServiceImpl(executor)

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            MtlsConfig.mutual(
                cert = NodeCertificateStorage.certificateFile(),
                key = NodeCertificateStorage.privateKeyFile(),
                caCert = NodeCertificateStorage.caCertificateFile(),
            )
        )
        .service(groupApiService)
        .build()

    fun start() = server.start()

    override fun close(mode: ShutdownMode) = server.close(mode)
}
