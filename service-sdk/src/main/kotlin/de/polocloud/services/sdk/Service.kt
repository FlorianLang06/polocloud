package de.polocloud.services.sdk

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseExecutor
import de.polocloud.services.sdk.communication.NodeConnection

/**
 * Base class for all PoloCloud services.
 *
 * Provides:
 * - [database] — direct access to the shared database executor
 * - [nodeConnection] — authenticated mTLS channel to the managing node
 *
 * Both are injected by [ServiceBoot] before [onBoot] is called, so they
 * are always available from that point on.
 *
 * ```kotlin
 * class MyService : Service() {
 *
 *     override fun onBoot() {
 *         val stub = NodeServiceGrpcKt.NodeServiceCoroutineStub(nodeConnection.channel())
 *         // call the node …
 *     }
 * }
 * ```
 */
abstract class Service {

    private val databaseExecutor : DatabaseExecutor = DatabaseAccess.executor()

    /**
     * Authenticated mTLS channel to the node that owns this service instance.
     * Injected by [ServiceBoot] before [onBoot] is called.
     */
    lateinit var nodeConnection: NodeConnection
        internal set

    /** Access to the shared database. */
    fun database() = databaseExecutor

    /**
     * Called once the service has booted and all infrastructure is ready.
     * Override to perform startup logic.
     */
    open fun onBoot() {}

    /**
     * Called when the service is shutting down.
     * Override to perform cleanup logic.
     */
    open fun onShutdown() {}

}