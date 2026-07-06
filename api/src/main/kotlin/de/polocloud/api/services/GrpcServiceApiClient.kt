package de.polocloud.api.services

import de.polocloud.proto.ServiceApiServiceGrpcKt
import de.polocloud.proto.ServiceData
import de.polocloud.proto.ServiceListRequest
import io.grpc.ManagedChannel
import java.util.concurrent.TimeUnit

/**
 * gRPC-backed [ServiceApiClient] that talks to the node's `ServiceApiService`.
 *
 * The channel is obtained lazily through [channelProvider] so the connection is
 * only opened when a call is actually made.
 */
class GrpcServiceApiClient(
    private val channelProvider: () -> ManagedChannel,
) : ServiceApiClient {

    // Every call carries a deadline so a misconfigured node surfaces as a clear
    // DEADLINE_EXCEEDED error instead of blocking the caller indefinitely.
    private fun stub() = ServiceApiServiceGrpcKt.ServiceApiServiceCoroutineStub(channelProvider())
        .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)

    override suspend fun findServices(groupFilter: String?, stateFilter: String?): List<ServiceData> {
        val request = ServiceListRequest.newBuilder().apply {
            groupFilter?.let { setGroupFilter(it) }
            stateFilter?.let { setStateFilter(it) }
        }.build()

        return stub().findServices(request).servicesList
    }

    private companion object {
        const val DEADLINE_SECONDS = 10L
    }
}
