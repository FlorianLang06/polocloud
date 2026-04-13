package de.polocloud.common.communication.client.call

import io.grpc.ManagedChannel

/**
 * Represents a single client-side gRPC call.
 */
interface GrpcClientCall<Response : Any> {

    suspend fun execute(channel: ManagedChannel): Response
}