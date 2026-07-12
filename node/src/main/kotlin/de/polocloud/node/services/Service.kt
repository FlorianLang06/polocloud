package de.polocloud.node.services

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import de.polocloud.node.group.Group
import de.polocloud.shared.service.ServiceState
import java.util.UUID

@RepositoryName("services")
open class Service(
    @EntryIdentifier val id: UUID,
    val index: Int,
    @EntryRef(clazz = Group::class) val groupName: String,
    var state: ServiceState = ServiceState.QUEUED,
    var hostname: String,
    var port: Int,
    // Id (as string) of the node that placed/owns this service. The `services` table is
    // conceptually per-node (peers are queried live over gRPC for their own local
    // services, never through this table), but nothing stops an operator from pointing
    // several nodes at one shared database backend (MySQL/Postgres/MongoDB are all
    // supported) instead of a private local file. This field lets node-scoped queries
    // (see ServiceRepository/ServiceProvider.reconcileStaleServices) stay correct even
    // then, instead of assuming every row in the table is this node's own.
    val nodeId: String = "",
) {

    fun name() = groupName + "-" + index
}
