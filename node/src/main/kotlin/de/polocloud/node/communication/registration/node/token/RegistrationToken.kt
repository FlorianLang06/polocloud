package de.polocloud.node.communication.registration.node.token

data class RegistrationToken(
    val token: String,
    val expiresAt: Long
)