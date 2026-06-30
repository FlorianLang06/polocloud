package de.polocloud.node.event

import de.polocloud.proto.EventContext
import de.polocloud.shared.event.Event
import de.polocloud.shared.event.EventCodec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Node-side fan-out for cluster events.
 *
 * Holds every open subscriber stream (one per `EventProvider.Subscribe` call) and
 * forwards matching [EventContext]s to them. Node-internal code publishes typed
 * events through [call]; the [de.polocloud.node.communication.impl.event.EventProviderServiceImpl]
 * relays remote publishes and subscriptions here.
 *
 * Implemented as a singleton so producers (e.g. the service factory) and the gRPC
 * service share the same registry without threading it through every constructor.
 */
object ClusterEventService {

    private val logger = LoggerFactory.getLogger(ClusterEventService::class.java)

    private class Subscriber(
        val eventName: String,
        val serviceName: String,
        val send: (EventContext) -> Unit,
    )

    private val subscribers = CopyOnWriteArrayList<Subscriber>()

    /**
     * Opens a stream for a remote subscriber. Emits every matching event until the
     * client disconnects, at which point the subscriber is removed automatically.
     *
     * An empty [eventName] subscribes to all events.
     */
    fun subscribe(eventName: String, serviceName: String): Flow<EventContext> = callbackFlow {
        val subscriber = Subscriber(eventName, serviceName) { context -> trySend(context) }
        subscribers += subscriber
        logger.debug("Event subscriber registered (event='{}', service='{}')", eventName, serviceName)

        awaitClose {
            subscribers.remove(subscriber)
            logger.debug("Event subscriber removed (event='{}', service='{}')", eventName, serviceName)
        }
    }

    /** Removes subscribers matching [eventName] and [serviceName] (explicit unsubscribe). */
    fun unsubscribe(eventName: String, serviceName: String) {
        subscribers.removeIf { it.eventName == eventName && it.serviceName == serviceName }
    }

    /** Broadcasts a pre-encoded context to all matching subscribers. */
    fun broadcast(context: EventContext) {
        subscribers
            .filter { it.eventName.isBlank() || it.eventName == context.eventName }
            .forEach { it.send(context) }
    }

    /** Encodes and broadcasts a typed [event] originating from the node. */
    fun call(event: Event) {
        val encoded = EventCodec.encode(event)
        broadcast(
            EventContext.newBuilder()
                .setEventName(encoded.name)
                .setEventData(encoded.data)
                .build()
        )
    }
}
