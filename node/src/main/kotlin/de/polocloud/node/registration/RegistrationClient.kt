package de.polocloud.node.registration

import de.polocloud.common.Address
import de.polocloud.common.error.exception.PoloResult
import de.polocloud.common.i18n.trInfo
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.node.error.NodeError
import de.polocloud.proto.NodeRegistrationServiceGrpcKt
import de.polocloud.proto.NodeVersion
import de.polocloud.proto.RegisterNodeRequest
import de.polocloud.proto.RegisterNodeResponse
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.UUID
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
    fun tryRegister(info: RegistrationInfo, localId: UUID, publicKey: String): PoloResult<RegisterNodeResponse> {
        val address = "${info.address.hostname}:${info.address.port}"
        val channel = createChannel(info.address)

        return runCatching {
            val stub = NodeRegistrationServiceGrpcKt.NodeRegistrationServiceCoroutineStub(channel)

            val request = RegisterNodeRequest.newBuilder()
                .setLocalId(localId.toString())
                .setHostname(info.address.hostname)
                .setPort(info.address.port)
                .setPublicKey(publicKey)
                .setDetails(
                    NodeVersion.newBuilder()
                        .setVersion(PolocloudVersion.CURRENT.toString())
                        .setGitHash(PolocloudVersion.CURRENT.commitId)
                        .build()
                )
                .setToken(info.token)
                .build()

            logger.trInfo("cluster","cluster.registration.sendingRequest", "address" to address)

            runBlocking { stub.registerNode(request) }

        }.also {
            shutdown(channel)
        }.getOrElse { ex ->
            return NodeError.RegistrationFailed(
                address = address,
                reason  = ex.message ?: "unknown"
            ).asFailure()
        }.let {
            Result.success(it)
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
    private fun createChannel(address: Address): ManagedChannel {
        return NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .usePlaintext() // Registration does not require TLS; secure communication is established after registration
            .build()
    }

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