package de.polocloud.node.services.process

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.services.control.ServiceControlPlan
import de.polocloud.proto.ProtoServiceProcessData
import de.polocloud.proto.ServiceState
import java.util.*

data class ServiceProcess(
    @EntryIdentifier val uuid: UUID,
    @EntryRef(clazz = ServiceControlPlan::class) val plan: String,
    @EntryRef(clazz = NodeData::class) val nodeID: UUID,
    val boundPort: Int,
    var pid: Int,
    var state: ServiceState,
) {

    fun changeState(state: ServiceState) {
        this.state = state
        this.update()
    }

    fun withRuntime(process: Process) {
        this.pid = process.pid().toInt()
        this.update()
    }

    private fun update() {
        ServiceProcessRepository.update(this)
    }
}

fun ServiceProcess.toProto(): ProtoServiceProcessData {
    return ProtoServiceProcessData.newBuilder()
        .setUuid(this.uuid.toString())
        .setPlan(this.plan)
        .setNodeId(this.nodeID.toString())
        .setBoundPort(this.boundPort)
        .setPid(this.pid)
        .setState(this.state)
        .build()
}