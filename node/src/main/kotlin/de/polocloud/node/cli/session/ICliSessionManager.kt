package de.polocloud.node.cli.session

/**
 * Manages active CLI sessions.
 *
 * A session is identified by the client's certificate subject (CN).
 * Sessions are created on first contact and refreshed on every subsequent request.
 */
interface ICliSessionManager {

    /**
     * Creates a new session or refreshes an existing one for the given subject.
     *
     * @param subject  Certificate CN of the CLI client (lowercased)
     * @param address  Remote IP address of the client
     * @return The created or updated [CliSession]
     */
    fun createOrUpdate(subject: String, address: String): CliSession

    /**
     * Updates the [CliSession.lastAccess] timestamp without changing the address.
     * No-op if no session exists for [subject].
     */
    fun touch(subject: String)

    /**
     * Removes the session for the given subject.
     * No-op if no session exists.
     */
    fun remove(subject: String)

    /**
     * Returns the session for the given subject, or null if not found.
     */
    fun get(subject: String): CliSession?

    /**
     * Returns a snapshot of all currently active sessions.
     */
    fun all(): Collection<CliSession>

    /**
     * Returns all sessions whose [CliSession.lastAccess] exceeds [timeout] milliseconds ago.
     */
    fun findExpired(timeout: Long): List<CliSession>

    /**
     * Removes all expired sessions.
     *
     * @param timeout Inactivity threshold in milliseconds
     */
    fun cleanupExpired(timeout: Long)
}