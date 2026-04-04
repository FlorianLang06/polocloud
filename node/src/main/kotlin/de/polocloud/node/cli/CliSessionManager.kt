package de.polocloud.node.cli

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CliSessionManager {

    private val sessions = ConcurrentHashMap<String, CliSession>()

    /**
     * Creates or updates a session for a CLI.
     *
     * - First request → new session
     * - Subsequent requests → refresh lastAccess + update address
     */
    fun createOrUpdate(subject: String, address: String, now: Long = now()): CliSession {
        return sessions.compute(subject) { _, existing ->
            existing?.copy(
                address = address,
                lastAccess = now
            ) ?: CliSession(
                sessionId = UUID.randomUUID().toString(),
                subject = subject,
                address = address,
                connectedAt = now,
                lastAccess = now
            )
        }!!
    }

    //TODO remove session on disconnect and create session also if cli already registered
    fun touch(subject: String, now: Long = now()) {
        sessions.computeIfPresent(subject) { _, session ->
            session.copy(lastAccess = now)
        }
    }

    fun remove(subject: String) {
        sessions.remove(subject.lowercase())
    }

    fun get(subject: String): CliSession? = sessions[subject]

    fun all(): Collection<CliSession> = sessions.values

    fun findExpired(timeout: Long, now: Long = now()): List<CliSession> {
        return sessions.values.filter { it.isExpired(timeout, now) }
    }

    /**
     * Removes all expired sessions.
     */
    fun cleanupExpired(timeout: Long, now: Long = now()) {
        sessions.entries.removeIf { it.value.isExpired(timeout, now) }
    }

    private fun now() = System.currentTimeMillis()
}