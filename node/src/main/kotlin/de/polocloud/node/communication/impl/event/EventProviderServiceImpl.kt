package de.polocloud.node.communication.impl.event

import de.polocloud.node.event.ClusterEventService
import de.polocloud.proto.CallEventResponse
import de.polocloud.proto.EventContext
import de.polocloud.proto.EventProviderGrpcKt
import de.polocloud.proto.EventSubscribeRequest
import de.polocloud.proto.UnsubscribeResponse
import kotlinx.coroutines.flow.Flow

/**
 * gRPC façade over [ClusterEventService] exposed on the service/plugin endpoint.
 *
 * Services and the proxy bridge open a server stream via [subscribe], publish via
 * [call], and tear down via [unsubscribe].
 */
class EventProviderServiceImpl : EventProviderGrpcKt.EventProviderCoroutineImplBase() {

    override fun subscribe(request: EventSubscribeRequest): Flow<EventContext> =
        ClusterEventService.subscribe(request.eventName, request.serviceName)

    override suspend fun call(request: EventContext): CallEventResponse {
        // Publish (not just broadcast) so an event fired through the SDK reaches the whole
        // cluster's subscribers, not only those connected to this node.
        ClusterEventService.publish(request)
        return CallEventResponse.newBuilder().setSuccess(true).build()
    }

    override suspend fun unsubscribe(request: EventSubscribeRequest): UnsubscribeResponse {
        ClusterEventService.unsubscribe(request.eventName, request.serviceName)
        return UnsubscribeResponse.newBuilder().setSuccess(true).build()
    }
}
