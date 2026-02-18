package dev.httpmarco.polocloud.database

import dev.httpmarco.polocloud.common.Address

open class DatabaseCredentials(
    val address: Address,
    val username: String,
    val password: String?
) {

}