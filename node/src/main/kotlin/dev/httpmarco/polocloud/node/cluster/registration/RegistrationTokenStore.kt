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

    private var token: Pair<String, String>? = null

    init {
        this.rotate()
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
            argon2.verify(token?.second, candidate.toCharArray())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Rotates the token: generates a new token and updates the hash.
     */
    fun rotate() {
        token = RegistrationTokenGenerator.generateToken()
    }
}