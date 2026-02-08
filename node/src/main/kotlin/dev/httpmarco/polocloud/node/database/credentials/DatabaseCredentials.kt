package dev.httpmarco.polocloud.node.database.credentials

abstract class DatabaseCredentials(
    val hostname: String,
    val port: Int,
    val username: String,
    val password: String
) {

}