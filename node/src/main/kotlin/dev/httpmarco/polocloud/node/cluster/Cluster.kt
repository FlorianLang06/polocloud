package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.common.utils.publicIpAddress
import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeData
import dev.httpmarco.polocloud.node.cluster.node.NodeState
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Cluster {

    private val clusterDatabaseKey = DatabaseKey("nodes", NodeData::class.java)

    private val logger = LoggerFactory.getLogger(Cluster.javaClass)
    private val database = NodeInstance.config.database.factory()

    init {
        database.connect()

        if (!database.isValid()) {
            logger.info(
                TranslationService.tr(
                    "cluster",
                    "cluster.node.database.failed"
                )
            )
            // if the database connection is not valid, we cannot continue. The node relies on the database for various operations, and without a valid connection, it cannot function properly.
            exitProcess(-1)
        }

        logger.info(
            TranslationService.tr(
                "cluster",
                "cluster.node.identity.detected",
                "nodeId" to NodeInstance.localId
            )
        )
    }

    fun detect() {
        val data = database.executor().findAll(clusterDatabaseKey)

        if(data.isEmpty()) {
            val ip = publicIpAddress()

            if(ip == null) {
                logger.info(
                    TranslationService.tr(
                        "cluster",
                        "cluster.node.identity.publicIpFailed"
                    )
                )
                // if we cannot determine our public IP address, we cannot continue. The node needs to know its public IP address to function properly, and without it, it cannot operate in the cluster.
                exitProcess(-1)
            }

            // we are the first node in the cluster, we need to create a new entry for ourselves in the database
            database.executor().save(clusterDatabaseKey, NodeData(NodeInstance.localId, "node-1", ip, 25565, NodeState.STARTING, true))
            logger.info(TranslationService.tr("cluster", "cluster.node.identity.created"))
        }
    }
}