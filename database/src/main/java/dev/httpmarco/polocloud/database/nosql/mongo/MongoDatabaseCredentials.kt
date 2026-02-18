package dev.httpmarco.polocloud.database.nosql.mongo

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.database.DatabaseCredentials

class MongoDatabaseCredentials(
    address: Address,
    username: String,
    password: String?,
    val database: String
) : DatabaseCredentials(address, username, password) {

}