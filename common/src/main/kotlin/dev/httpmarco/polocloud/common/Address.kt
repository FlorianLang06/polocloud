package dev.httpmarco.polocloud.common

val GLOBAL_ADDRESS = Address("0.0.0.0", 1)

/**
 * Represents a network address (hostname + port).
 *
 * Can be used for GRPC endpoints, database connections, or internal node communication.
 *
 * @property hostname the host or IP address
 * @property port the network port
 */
data class Address(
    val hostname: String,
    val port: Int
) {

    init {
        require(port in 1..65535) { "Port must be between 1 and 65535, but was $port" }
        require(hostname.isNotBlank()) { "Hostname cannot be blank" }
    }

    /**
     * Returns the address as a string in `hostname:port` format.
     */
    fun asString(): String = "$hostname:$port"

    /**
     * Returns a copy of this address with a different port.
     */
    fun withPort(newPort: Int): Address = copy(port = newPort)

    /**
     * Returns a copy of this address with a different hostname.
     */
    fun withHostname(newHostname: String): Address = copy(hostname = newHostname)
}
