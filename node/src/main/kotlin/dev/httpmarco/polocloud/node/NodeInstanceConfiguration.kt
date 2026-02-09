package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.Address
import dev.httpmarco.polocloud.node.cluster.external.database.credentials.DatabaseCredentials

data class NodeInstanceConfiguration(val nodeId: String, val bindAddress: Address, val database: DatabaseCredentials) {


}