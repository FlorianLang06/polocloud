package de.polocloud.api.services

import de.polocloud.proto.ServiceData

/**
 * Maps the protobuf [ServiceData] wire type to the public API [Service].
 */
object ServiceMapper {

    fun toApi(data: ServiceData): Service = Service(
        id = data.id,
        index = data.index,
        group = data.group,
        state = data.state,
        port = data.port,
        pid = data.pid,
    )
}
