package de.polocloud.cli.communication.client.impl.services

import de.polocloud.cli.communication.client.call.services.ListServicesCall
import de.polocloud.common.communication.client.executor.GrpcClientExecutor
import de.polocloud.proto.ProtoServiceProcessData

class ServiceClientImpl(
    private val executor: GrpcClientExecutor
) {

    suspend fun listServices(): List<ProtoServiceProcessData> {
        return executor.execute(ListServicesCall())
    }
}