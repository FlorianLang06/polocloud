package dev.httpmarco.polocloud.node.cluster.registration

import de.mkammerer.argon2.Argon2Factory

/**
 * Stores and validates the current registration token using Argon2 hashing.
 *
 * The token is generated as a secure random string, hashed with Argon2,
 * and only the hash is stored internally. Verification is always performed
 * against the hash using Argon2.
 */
class RegistrationTokenStore {

    private var tokenHash: String
    private var tokenPlain: String

    init {
        val token = RegistrationTokenGenerator.generateToken()
        tokenPlain = token.first
        tokenHash = token.second
    }

    private val argon2 = Argon2Factory.create()

    /**
     * Verifies a candidate token using Argon2 against the stored hash.
     *
     * @param candidate The token provided by a node.
     * @return true if the token is valid, false otherwise.
     */
    fun verify(candidate: String): Boolean {
        return try {
            argon2.verify(tokenHash, candidate.toCharArray())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Rotates the token: generates a new token and updates the hash.
     */
    fun rotate() {
        val token = RegistrationTokenGenerator.generateToken()
        tokenPlain = token.first
        tokenHash = token.second
    }
}