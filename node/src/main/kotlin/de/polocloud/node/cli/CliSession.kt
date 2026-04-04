package de.polocloud.node.cli

data class CliSession(
    val sessionId: String,
    val subject: String,
    val address: String,
    val connectedAt: Long,
    var lastAccess: Long,
) {
    fun touch(now: Long): CliSession {
        return copy(lastAccess = now)
    }

    fun isExpired(timeout: Long, now: Long): Boolean {
        return now - lastAccess > timeout
    }
}