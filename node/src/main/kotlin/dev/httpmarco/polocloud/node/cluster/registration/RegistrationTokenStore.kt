package dev.httpmarco.polocloud.node.cluster.registration

import de.mkammerer.argon2.Argon2Factory

/**
 * Stores and validates the cluster registration token.
 *
 * The token is generated as a secure random value and hashed using Argon2.
 * Only the hash is used for verification, while the plain token is exposed
 * temporarily for bootstrap purposes (e.g., CLI output or integration tests).
 */
class RegistrationTokenStore {

    private val argon2 = Argon2Factory.create()

    private var token: Token

    init {
        token = generate()
    }

    /**
     * Verifies a candidate token against the stored Argon2 hash.
     *
     * @param candidate token provided by a joining node
     * @return true if the token is valid
     */
    fun verify(candidate: String): Boolean {
        return try {
            argon2.verify(token.hash, candidate.toCharArray())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Rotates the registration token and generates a new one.
     */
    fun rotate() {
        token = generate()
    }

    /**
     * Returns the current plain registration token.
     *
     * Intended for bootstrap scenarios (tests, CLI output).
     */
    fun currentToken(): String {
        return token.plain
    }

    private fun generate(): Token {
        val (plain, hash) = RegistrationTokenGenerator.generateToken()
        return Token(plain, hash)
    }

    private data class Token(
        val plain: String,
        val hash: String
    )
}