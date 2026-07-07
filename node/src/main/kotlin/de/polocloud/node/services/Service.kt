package de.polocloud.node.services

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.database.RepositoryName
import de.polocloud.node.group.Group
import java.util.UUID

@RepositoryName("services")
open class Service(
    @EntryIdentifier val id: UUID,
    val index: Int,
    @EntryRef(clazz = Group::class) val group: String,
    var state: ServiceState = ServiceState.QUEUED,
) {

    fun name() = group + "-" + index
}
