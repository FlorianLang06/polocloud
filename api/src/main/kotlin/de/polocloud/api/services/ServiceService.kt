package de.polocloud.api.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Public, blocking entry point to the cluster's service API.
 *
 * Backed by a [ServiceApiClient] (gRPC in production). Obtain the shared instance
 * via [de.polocloud.api.Polocloud.serviceService].
 *
 * Calls run on [Dispatchers.IO] rather than the caller's thread: callers such as
 * the proxy bridge invoke these from a platform lifecycle thread, and confining
 * the suspending gRPC call to that thread can deadlock when the same thread is
 * needed to resume the response continuation.
 */
class ServiceService internal constructor(
    private val client: ServiceApiClient,
) {

    /** All services currently known to the cluster. */
    fun findAll(): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, null) }.map(ServiceMapper::toApi)

    /** The service with the given `group-index` [name], or `null` if none matches. */
    fun find(name: String): Service? =
        findAll().firstOrNull { it.name().equals(name, ignoreCase = true) }

    /** All services belonging to [group]. */
    fun findByGroup(group: String): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(group, null) }.map(ServiceMapper::toApi)

    /** All services currently in [state] (e.g. `RUNNING`). */
    fun findByState(state: String): List<Service> =
        runBlocking(Dispatchers.IO) { client.findServices(null, state) }.map(ServiceMapper::toApi)

    /** Number of services currently known to the cluster. */
    fun count(): Int = findAll().size

    /** Number of services belonging to [group]. */
    fun count(group: String): Int = findByGroup(group).size
}
