package de.polocloud.node.forwarding

import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

/**
 * Owns the cluster's player-forwarding secret — the single shared token a proxy
 * (Velocity "modern" forwarding) and every backend server must agree on so the
 * proxy can vouch for a connecting player's identity.
 *
 * The secret is generated once and persisted to [secretFile], so it stays stable
 * across node restarts: a proxy that is already running keeps accepting players
 * when an individual backend server is restarted with the same token.
 *
 * The value is exposed to configuration tasks through the `%FORWARDING_SECRET%`
 * placeholder (see [de.polocloud.node.services.factory.FactoryService]), which
 * writes it into e.g. `config/paper-global.yml` (`proxies.velocity.secret`) and
 * the proxy's own forwarding configuration.
 */
class ForwardingHandler(
    private val secretFile: File = DEFAULT_SECRET_FILE,
) {

    private val logger = LoggerFactory.getLogger(ForwardingHandler::class.java)

    /** Forwarding mode used for services started by this node. */
    val state: ForwardingState = detectForwarding()

    /** The shared forwarding secret, loaded from disk or freshly generated once. */
    val secret: String = loadOrCreateSecret()

    private fun detectForwarding(): ForwardingState = ForwardingState.MODERN

    /**
     * Returns the persisted secret if a non-empty one exists, otherwise generates a
     * new one and best-effort persists it. A failure to persist is logged and the
     * in-memory secret is used, so a read-only disk never blocks a service start.
     */
    private fun loadOrCreateSecret(): String {
        val existing = runCatching { secretFile.takeIf { it.isFile }?.readText()?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
        if (existing != null) return existing

        val generated = UUID.randomUUID().toString().replace("-", "")
        runCatching {
            secretFile.parentFile?.mkdirs()
            secretFile.writeText(generated)
        }.onFailure {
            logger.warn("Could not persist forwarding secret to {} — using an in-memory secret", secretFile.path, it)
        }
        return generated
    }

    private companion object {
        val DEFAULT_SECRET_FILE = File("local/forwarding.secret")
    }
}