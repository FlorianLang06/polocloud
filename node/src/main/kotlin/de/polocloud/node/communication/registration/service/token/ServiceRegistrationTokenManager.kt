package de.polocloud.node.communication.registration.service.token

import de.polocloud.node.security.CSPRNGGenerator
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Issues and validates short-lived, single-use registration tokens for service instances.
 *
 * Each token is bound to a specific [serviceId] so a stolen token cannot be
 * used to register a different service process.
 *
 * Tokens expire after [ttlMs] (default 60 s — enough time for the JVM to start
 * and complete the registration handshake).
 */
class ServiceRegistrationTokenManager(private val ttlMs: Long = 60_000L) {

    private data class Entry(val token: String, val serviceId: UUID, val expiresAt: Long)

    private val tokens = ConcurrentHashMap<String, Entry>()

    /**
     * Creates a new single-use token bound to [serviceId].
     * @return the raw token string to be passed as `-Dservice.token=<token>`
     */
    fun issue(serviceId: UUID): String {
        val token = CSPRNGGenerator.generate()
        tokens[token] = Entry(token, serviceId, System.currentTimeMillis() + ttlMs)
        return token
    }

    /**
     * Validates [token] against [serviceId].
     *
     * - Returns `true` and **consumes** the token on success (single-use).
     * - Returns `false` if the token is unknown, expired, or bound to a different service.
     */
    fun validate(token: String, serviceId: UUID): Boolean {
        val entry = tokens.remove(token) ?: return false

        if (System.currentTimeMillis() > entry.expiresAt) return false
        if (entry.serviceId != serviceId) return false

        return true
    }
}