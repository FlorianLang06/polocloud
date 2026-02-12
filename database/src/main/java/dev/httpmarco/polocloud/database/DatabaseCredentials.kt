package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Address

abstract class DatabaseCredentials(
    val address: Address,
    val username: String,
    val password: String
) {

    abstract fun type() : DatabaseType

}