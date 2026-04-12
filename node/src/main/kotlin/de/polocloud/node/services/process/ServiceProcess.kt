package de.polocloud.node.services.process

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.EntryRef
import de.polocloud.node.cluster.node.NodeData
import de.polocloud.node.services.ServiceState
import de.polocloud.node.services.control.ServiceControlPlan
import java.util.UUID

data class ServiceProcess(
    @EntryIdentifier val uuid: UUID,
    @EntryRef(clazz = ServiceControlPlan::class) val plan: String,
    @EntryRef(clazz = NodeData::class) val nodeID: UUID,
    val boundPort: Int,
    var pid: Int,
    private var state: ServiceState,
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