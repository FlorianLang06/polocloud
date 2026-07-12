package de.polocloud.bridge

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Picks the best fallback [Service] to route a player to, given the currently known
 * fallback groups and their priorities.
 *
 * Pure/stateless so it can be exercised directly in tests without wiring up the full
 * [BridgeBootstrap] (which requires a live [de.polocloud.api.Polocloud] connection).
 */
object FallbackSelector {

    /**
     * @param services candidate services to choose from, e.g. everything registered on
     *   the proxy.
     * @param fallbackGroups fallback group names (lowercase) mapped to their priority.
     * @param excludeServiceName a service name to skip, e.g. the server a player was
     *   just kicked from, so they are never redirected right back to it.
     * @return among running, eligible services, the one in the highest priority tier
     *   that has a running service; within that tier, the one with the fewest online
     *   players. `null` if none is eligible.
     */
    fun select(
        services: List<Service>,
        fallbackGroups: Map<String, Int>,
        excludeServiceName: String? = null,
    ): Service? {
        val candidates = services.filter { service ->
            service.state == ServiceState.RUNNING &&
                (excludeServiceName == null || !service.name().equals(excludeServiceName, ignoreCase = true)) &&
                (service.group.lowercase() in fallbackGroups || service.isFallback())
        }

        val topPriority = candidates.maxOfOrNull { priorityOf(it, fallbackGroups) } ?: return null
        return candidates
            .filter { priorityOf(it, fallbackGroups) == topPriority }
            .minByOrNull { it.onlinePlayers }
    }

    /** A service's own fallback priority, or its group's if the service itself carries none. */
    private fun priorityOf(service: Service, fallbackGroups: Map<String, Int>): Int =
        if (service.isFallback()) service.fallbackPriority() else fallbackGroups[service.group.lowercase()] ?: 0
}