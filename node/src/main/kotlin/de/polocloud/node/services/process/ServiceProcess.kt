package de.polocloud.node.services.process

import de.polocloud.node.services.ServiceState
import java.util.UUID

data class ServiceProcess(val uuid: UUID, val boundPort : Int, var pid : Int, var state: ServiceState) {



}