package de.polocloud.api.services

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Public, blocking entry point to the service API.
 *
 * Backed by a [ServiceApiClient] (gRPC in production). Obtain the shared instance
 * via [de.polocloud.api.Polocloud.serviceService].
 *
 * The results are cluster-wide: the connected node aggregates its own local services
 * with every other online node's local services before responding, so a proxy or
 * plugin only ever needs to talk to one node to see the whole cluster (see
 * `FindServicesServerHandler` on the node side).
 *
 * Calls run on [Dispatchers.IO] rather than the caller's thread: callers such as
 * the proxy bridge invoke these from a platform lifecycle thread, and confining
 * the suspending gRPC call to that thread can deadlock when the same thread is
 * needed to resume the response continuation.
 */
class ServiceService internal constructor(
    private val client: ServiceApiClient,
) {

    /** All services currently known to the connected node. */
    fun findAll(): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, null) }.map(ServiceMapper::toApi)

    /** The service with the given `group-index` [name], or `null` if none matches. */
    fun find(name: String): Service? =
        findAll().firstOrNull { it.name().equals(name, ignoreCase = true) }

    /** All services belonging to [group]. */
    fun findByGroup(group: String): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(group, null) }.map(ServiceMapper::toApi)

    /** All services currently in [state] (e.g. [ServiceState.RUNNING]). */
    fun findByState(state: ServiceState): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, state.name) }.map(ServiceMapper::toApi)

    /**
     * Number of services currently known to the connected node.
     *
     * Skips mapping to [Service] since only the count is needed. A dedicated
     * server-side count RPC would avoid transferring the full list; add one if
     * this becomes a hot path.
     */
    fun count(): Int = runBlocking(Dispatchers.IO) { client.findServices(null, null) }.size

    /** Number of services belonging to [group]. */
    fun count(group: String): Int = runBlocking(Dispatchers.IO) { client.findServices(group, null) }.size
}
