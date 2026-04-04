package de.polocloud.node.cli

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CliSessionManager {

    private val sessions = ConcurrentHashMap<String, CliSession>()

    fun create(subject: String, address: String, now: Long = now()): CliSession {
        return sessions.computeIfAbsent(subject) {
            CliSession(
                sessionId = UUID.randomUUID().toString(),
                subject = subject,
                address = address,
                connectedAt = now,
                lastAccess = now
            )
        }//TODO remove session on disconnect and create session also if cli already registered
    }

    fun touch(subject: String, now: Long = now()) {
        sessions.computeIfPresent(subject) { _, session ->
            session.touch(now)
        }
    }

    fun remove(subject: String) {
        sessions.remove(subject)
    }

    fun get(subject: String): CliSession? = sessions[subject]

    fun all(): Collection<CliSession> = sessions.values

    fun findExpired(timeout: Long, now: Long = now()): List<CliSession> {
        return sessions.values.filter { it.isExpired(timeout, now) }
    }

    private fun now() = System.currentTimeMillis()
}