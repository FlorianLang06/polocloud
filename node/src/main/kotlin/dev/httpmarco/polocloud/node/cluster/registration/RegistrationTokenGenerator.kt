package dev.httpmarco.polocloud.node.cluster.registration

import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import java.util.Base64

/**
 * Utility class for generating and verifying cryptographically secure registration tokens
 * for nodes joining a cluster.
 *
 * <p>
 * Each token is randomly generated and hashed using Argon2 (ARGON2id) for secure storage.
 * The raw token is sent to the node, while only the hash is persisted.
 * </p>
 */
class RegistrationTokenGenerator {

    companion object {

        private val secureRandom = SecureRandom()
        private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

        /**
         * Generates a new cryptographically secure registration token along with its Argon2 hash.
         *
         * @return a [Pair] containing:
         *   - first: the raw token to be sent to the node
         *   - second: the Argon2 hash for secure storage and verification
         */
        fun generateToken(): Pair<String, String> {
            // Generate 32 bytes (256-bit) random token
            val bytes = ByteArray(32)
            secureRandom.nextBytes(bytes)
            val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

            // Hash the token with Argon2id (3 iterations, 64 MB memory, 1 parallelism)
            val hash = argon2.hash(3, 65536, 1, token.toCharArray())

            return token to hash
        }

        /**
         * Verifies that a raw token matches a stored Argon2 hash.
         *
         * @param token the raw token to verify
         * @param hash the stored Argon2 hash
         * @return true if the token matches the hash, false otherwise
         */
        fun verifyToken(token: String, hash: String): Boolean {
            return argon2.verify(hash, token.toCharArray())
        }
    }
}