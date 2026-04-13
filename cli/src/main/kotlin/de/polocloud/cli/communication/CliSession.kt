package de.polocloud.cli.communication

import de.polocloud.cli.communication.client.CliGrpcClientModule
import de.polocloud.cli.communication.client.impl.cluster.ClusterClientImpl
import de.polocloud.cli.communication.client.impl.node.NodeClientImpl
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.common.communication.client.executor.GrpcClientExecutor

class CliSession(
    connectionManager: CliConnectionManager
) {

    private val executor: GrpcClientExecutor = CliGrpcClientModule.createExecutor(connectionManager)

    val clusterClient = ClusterClientImpl(executor)
    val nodeClient = NodeClientImpl(executor)
}