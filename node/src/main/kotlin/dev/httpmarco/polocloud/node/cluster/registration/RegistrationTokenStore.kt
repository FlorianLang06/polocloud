package dev.httpmarco.polocloud.node.cluster.registration

/**
 * Stores and validates the current registration token.
 */
class RegistrationTokenStore {

    private var token: String = RegistrationTokenGenerator.generateToken()

    fun validate(candidate: String): Boolean {
        return token == candidate
    }

    fun rotate() {
        token = RegistrationTokenGenerator.generateToken()
    }
}