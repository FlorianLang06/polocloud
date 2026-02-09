package dev.httpmarco.polocloud.node.cluster.external.database.credentials

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.node.cluster.external.database.DatabaseType

class SqlDatabaseCredentials(
    val driver: String,
    address: Address,
    username: String,
    password: String,
    val database: String
) : DatabaseCredentials(address, username, password) {
    override fun type() = DatabaseType.SQL
}