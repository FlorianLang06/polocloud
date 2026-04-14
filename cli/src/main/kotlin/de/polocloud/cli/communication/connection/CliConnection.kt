package de.polocloud.cli.communication.connection

import de.polocloud.common.Address
import io.grpc.ManagedChannel

/**
 * Abstraction over the CLI's connection to the cluster.
 *
 * Two ports are involved:
 * - [registrationAddress] — plaintext, used once for the initial registration handshake
 * - [clusterAddress]      — mTLS, used for all subsequent cluster communication
 */
interface CliConnection {

    val isConnected: Boolean

    /**
     * Ensures the CLI is registered and opens the mTLS channel.
     *
     * If not yet registered, the registration handshake runs on [registrationAddress] (plaintext).
     * The persistent mTLS channel is then opened on [clusterAddress].
     *
     * Calling this when already connected is a no-op.
     */
    suspend fun connect(clusterAddress: Address, registrationAddress: Address, token: String? = null)

    /**
     * Returns the active [ManagedChannel] for creating gRPC service stubs.
     * @throws IllegalStateException if not connected
     */
    fun channel(): ManagedChannel

    fun disconnect()
}