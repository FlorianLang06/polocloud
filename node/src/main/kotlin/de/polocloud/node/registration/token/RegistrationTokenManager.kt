package de.polocloud.node.registration.token

import de.polocloud.node.generator.CSPRNGGenerator
import java.util.concurrent.ConcurrentHashMap

class RegistrationTokenManager {

    private val tokens = ConcurrentHashMap<String, RegistrationToken>()

    fun create(ttlMs: Long): RegistrationToken {
        val token = CSPRNGGenerator.generate()
        val entry = RegistrationToken(token, now() + ttlMs)

        tokens[token] = entry
        return entry
    }

    fun validate(token: String): Boolean {
        val entry = tokens[token] ?: return false

        if (entry.expiresAt < now()) {
            tokens.remove(token)
            return false
        }

        tokens.remove(token)
        return true
    }

    private fun now() = System.currentTimeMillis()

    fun createInitialCliToken(): RegistrationToken {
        return create(ttlMs = 10 * 60 * 1000L)
    }
}