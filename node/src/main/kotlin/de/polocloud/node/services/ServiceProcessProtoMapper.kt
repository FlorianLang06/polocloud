package de.polocloud.node.services

import de.polocloud.proto.ProtoServiceProcessData
import de.polocloud.proto.ServiceState as ProtoServiceState

/**
 * Maps a running [LocalService] to the CLI-facing [ProtoServiceProcessData] wire type
 * (consumed by the `service` command). Mirrors [ServiceProtoMapper], which maps the
 * same source to the API-facing `ServiceData`.
 */
object ServiceProcessProtoMapper {

    fun toProto(service: LocalService): ProtoServiceProcessData = ProtoServiceProcessData.newBuilder()
        .setUuid(service.id.toString())
        .setIndex(service.index)
        .setPlan(service.groupName)
        .setBoundPort(service.port)
        .setPid((service.process?.pid() ?: -1L).toInt())
        .setState(toProtoState(service.state))
        .putAllProperties(service.properties)
        .build()

    /**
     * Maps the node's lifecycle [ServiceState] to the coarser protobuf `common.ServiceState`.
     *
     * The two enums do not line up one-to-one (protobuf has no dedicated stopped state),
     * so transient/terminal node states fall back to the closest protobuf value.
     */
    private fun toProtoState(state: ServiceState): ProtoServiceState = when (state) {
        ServiceState.QUEUED -> ProtoServiceState.LOADING
        ServiceState.STARTING -> ProtoServiceState.BOOTING
        ServiceState.RUNNING -> ProtoServiceState.RUNNING
        ServiceState.STOPPING, ServiceState.STOPPED -> ProtoServiceState.UNCONTROLLED
    }
}
