package de.polocloud.node.registration.token

data class RegistrationToken(
    val token: String,
    val expiresAt: Long
)