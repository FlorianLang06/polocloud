package de.polocloud.node.communication.registration.client

import de.polocloud.common.Address
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.i18n.api.trInfo
import de.polocloud.node.communication.registration.node.RegistrationInfo
import de.polocloud.proto.NodeRegistrationServiceGrpcKt
import de.polocloud.proto.NodeVersion
import de.polocloud.proto.RegisterNodeRequest
import de.polocloud.proto.RegisterNodeResponse
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Client responsible for registering a node with a cluster via gRPC.
 *
 * <p>This client builds a registration request containing node details,
 * establishes a gRPC channel to the target node, sends the request,
 * and then gracefully shuts down the channel.</p>
 */
class RegistrationClient {

    private val logger = LoggerFactory.getLogger(RegistrationClient::class.java)

    /**
     * Attempts to register this node with a target cluster node.
     *
     * <p>This method creates a gRPC channel to the target node, sends a
     * RegisterNodeRequest, and waits for the RegisterNodeResponse.
     * The channel is always shut down after the request completes.</p>
     *
     * @param info Registration information including address and token.
     * @param localId The unique UUID of this node.
     * @param publicKey The node's public key for secure communication.
     * @return The response from the cluster node registration service.
     */
    fun tryRegister(info: RegistrationInfo, localId: UUID, publicKey: String): RegisterNodeResponse {
        val address = "${info.address.hostname}:${info.address.port}"
        val channel = createChannel(info.address)

        try {
            val stub = NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineStub(channel)

            val request = RegisterNodeRequest.newBuilder()
                .setLocalId(localId.toString())
                .setHostname(info.address.hostname)
                .setPort(info.address.port)
                .setPublicKey(publicKey)
                .setDetails(
                    NodeVersion.newBuilder()
                        .setVersion(PolocloudVersion.Companion.CURRENT.toString())
                        .setGitHash(PolocloudVersion.Companion.CURRENT.commitId)
                        .build()
                )
                .setToken(info.token)
                .build()

            logger.trInfo("cluster", "cluster.registration.sendingRequest", "address" to address)

            return runBlocking { stub.registerNode(request) }

        } catch (exception: Exception) {
            throw IllegalStateException("Failed to register with cluster node at '$address'", exception)
        } finally {
            shutdown(channel)
        }
    }

    /**
     * Creates a gRPC channel to the target cluster node.
     *
     * <p>The channel uses plaintext initially, as registration does not require TLS.</p>
     *
     * @param address The hostname and port of the target cluster node.
     * @return A managed gRPC channel.
     */
    private fun createChannel(address: Address) = NettyChannelBuilder
        .forAddress(address.hostname, address.port)
        .usePlaintext() // Registration does not require TLS; secure communication is established after registration
        .build()

    /**
     * Gracefully shuts down a gRPC channel.
     *
     * <p>Waits up to 3 seconds for termination to complete.</p>
     *
     * @param channel The channel to shut down.
     */
    private fun shutdown(channel: ManagedChannel) {
        channel.shutdown()
        channel.awaitTermination(3, TimeUnit.SECONDS)
    }
}