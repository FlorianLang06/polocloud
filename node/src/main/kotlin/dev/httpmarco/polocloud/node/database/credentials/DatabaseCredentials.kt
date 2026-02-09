package dev.httpmarco.polocloud.node.database.credentials

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.node.database.DatabaseType

abstract class DatabaseCredentials(
    val address: Address,
    val username: String,
    val password: String
) {

    abstract fun type() : DatabaseType

}