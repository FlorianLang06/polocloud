package dev.httpmarco.polocloud.node.cluster.registration

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.cluster.security.ClusterSecurity
import dev.httpmarco.polocloud.node.cluster.security.toBase64
import dev.httpmarco.polocloud.proto.NodeRegistrationServiceGrpcKt
import dev.httpmarco.polocloud.proto.NodeVersion
import dev.httpmarco.polocloud.proto.RegisterNodeRequest
import dev.httpmarco.polocloud.proto.RegisterNodeResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Client responsible for registering a new node with a cluster.
 *
 * Uses gRPC to communicate with the cluster's registration service.
 * Handles creation of the registration request, sending it to the cluster,
 * and receiving the registration response.
 *
 * @property security The local node's security context providing nodeId and keys.
 */
class RegistrationClient(val security: ClusterSecurity) {

    private val logger = LoggerFactory.getLogger(RegistrationClient::class.java)

    /**
     * Registers this node with a cluster using the provided registration information.
     *
     * @param info The registration information containing the cluster address and token.
     * @return The response from the cluster registration service.
     */
    fun register(info: RegistrationInfo): RegisterNodeResponse {
        val channel = createChannel(info.address)

        return try {
            val stub = NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineStub(channel)

            val request = RegisterNodeRequest.newBuilder()
                .setLocalId(security.localId.toString())
                .setHostname(info.address.hostname)
                .setPort(info.address.port)
                .setPublicKey(security.publicKey.toBase64())
                // TODO: Set the actual version and git hash
                .setDetails(NodeVersion.newBuilder().setVersion("RECHER").setGitHash("GG").build())
                .setToken(info.token)
                .build()

            logger.info(
                TranslationService.tr(
                    "cluster",
                    "cluster.registration.sendingRequest",
                    "address" to "${info.address.hostname}:${info.address.port}"
                )
            )

            runBlocking { stub.registerNode(request) }
        } finally {
            shutdown(channel)
        }
    }

    /**
     * Creates a gRPC channel to the target cluster node.
     *
     * @param address The hostname and port of the target cluster node.
     * @return A managed gRPC channel.
     */
    private fun createChannel(address: Address): ManagedChannel {
        return ManagedChannelBuilder
            .forAddress(address.hostname, address.port)
            .usePlaintext() // Registration initially without TLS
            .build()
    }

    /**
     * Gracefully shuts down a gRPC channel.
     *
     * @param channel The channel to shut down.
     */
    private fun shutdown(channel: ManagedChannel) {
        channel.shutdown()
        channel.awaitTermination(3, TimeUnit.SECONDS)
    }
}