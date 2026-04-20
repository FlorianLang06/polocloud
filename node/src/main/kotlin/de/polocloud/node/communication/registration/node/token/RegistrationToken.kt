package de.polocloud.node.communication.registration.node.token

data class RegistrationToken(
    val token: String,
    val expiresAt: Long
)

fun RegistrationToken.toProto(): ProtoRegistrationToken {
    return ProtoRegistrationToken.newBuilder()
        .setToken(token)
        .setExpiresAt(expiresAt)
        .build()
}