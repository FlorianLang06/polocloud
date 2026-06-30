package de.polocloud.shared.event

/**
 * Marker interface for everything that can be dispatched through the cluster
 * event bus.
 *
 * Concrete events are `@Serializable` data classes carrying the relevant
 * payload, e.g. [de.polocloud.shared.event.server.ServerStartedEvent]. They live
 * in `shared` so both the node (producer) and the api/bridge (consumer) can use
 * them without depending on each other.
 *
 * Every event must be registered in [EventRegistry] so it can be (de)serialized
 * across the wire.
 */
interface Event
