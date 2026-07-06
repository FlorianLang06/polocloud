package de.polocloud.api.services

/**
 * A running service in the cluster, as exposed through the public API.
 *
 * Obtain instances via [de.polocloud.api.Polocloud.serviceService].
 */
data class Service(
    val id: String,
    val index: Int,
    val group: String,
    val state: String,
    val port: Int,
    val pid: Long,
) {

    /** Cluster-wide service name, e.g. `lobby-1`. */
    fun name() = "$group-$index"
}
