package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.node.database.credentials.DatabaseCredentials

data class LocalNodeConfiguration(val nodeId: String, val bindAddress: Address, val database: DatabaseCredentials) {


}