package de.polocloud.cli.command.impl.cluster

import de.polocloud.cli.Cli
import de.polocloud.cli.communication.client.impl.cluster.ClusterClientImpl
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.KeywordArgument
import de.polocloud.cli.communication.client.CliGrpcClientModule
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.logger
import kotlinx.coroutines.runBlocking

class NodesCommand(
    private val connectionManager: CliConnectionManager
) : Command("nodes", "List all nodes in the cluster") {

    init {
        syntax(
            {
                if (!connectionManager.isConnected) {
                    logger.info("Not connected.")
                    return@syntax
                }

                runBlocking {
                    val nodes = Cli.session.clusterClient.listNodes()

                    if (nodes.isEmpty()) {
                        logger.info("No nodes found.")
                        return@runBlocking
                    }

                    logger.info("Cluster Nodes:")
                    nodes.forEach { node ->
                        logger.info(node)
                        logger.info(
                            "${node.groupName}-${node.index} " +
                                    "(${node.hostname}:${node.port}) " +
                                    "[${node.state}]"
                        )
                    }
                }
            },
            KeywordArgument("list")
        )
    }
}