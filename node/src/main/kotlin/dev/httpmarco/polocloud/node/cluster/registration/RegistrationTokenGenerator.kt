package dev.httpmarco.polocloud.node.cluster.registration

import java.security.SecureRandom
import java.util.Base64

class RegistrationTokenGenerator {

    companion object {

        private val secureRandom = SecureRandom()

        /**
         * Generates a cryptographically secure registration token.
         *
         * The token is used during node bootstrap to authenticate
         * new nodes before TLS certificates are issued.
         *
         * @return secure random token encoded as URL-safe Base64 string
         */
        fun generateToken(): String {
            val bytes = ByteArray(32) // 256 bit
            secureRandom.nextBytes(bytes)

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)
        }
    }
}