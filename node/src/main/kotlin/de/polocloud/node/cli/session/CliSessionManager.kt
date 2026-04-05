package de.polocloud.node.cli.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CliSessionManager : ICliSessionManager {

    private val sessions = ConcurrentHashMap<String, CliSession>()

    override fun createOrUpdate(subject: String, address: String): CliSession {
        val now = now()
        return sessions.compute(subject) { _, existing ->
            existing?.copy(address = address, lastAccess = now)
                ?: CliSession(
                    sessionId  = UUID.randomUUID().toString(),
                    subject    = subject,
                    address    = address,
                    connectedAt = now,
                    lastAccess = now,
                )
        }!!
    }

    //TODO save sessions into db?!
    //TODO create session also if cli already registered
    override fun touch(subject: String) {
        val now = now()
        sessions.computeIfPresent(subject) { _, session -> session.copy(lastAccess = now) }
    }

    override fun remove(subject: String) {
        sessions.remove(subject.lowercase())
    }

    override fun get(subject: String): CliSession? = sessions[subject]

    override fun all(): Collection<CliSession> = sessions.values.toList()

    override fun findExpired(timeout: Long): List<CliSession> {
        val now = now()
        return sessions.values.filter { it.isExpired(timeout, now) }
    }

    override fun cleanupExpired(timeout: Long) {
        val now = now()
        sessions.entries.removeIf { it.value.isExpired(timeout, now) }
    }

    private fun now(): Long = System.currentTimeMillis()
}