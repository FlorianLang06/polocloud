package de.polocloud.node.services

import de.polocloud.shared.service.Service
import de.polocloud.shared.service.ServiceState

/**
 * Maps a running [LocalService] to the shared [Service] model carried on cluster
 * lifecycle events (e.g. [de.polocloud.shared.event.server.ServerStartedEvent]).
 *
 * Mirrors [ServiceProtoMapper], which maps the same source to the protobuf wire
 * type used by the gRPC API.
 */
object ServiceEventMapper {

    fun toShared(service: LocalService): Service = Service(
        id = service.id.toString(),
        index = service.index,
        group = service.groupName,
        state = ServiceState.fromWire(service.state.name),
        port = service.port,
        host = service.host,
        pid = service.process?.pid() ?: -1L,
    )
}