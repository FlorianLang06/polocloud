package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.database.DatabaseKey
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.NodeInstance
import dev.httpmarco.polocloud.node.cluster.node.NodeData
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
        detect()

    }

    fun detect() {
        val data = database.executor().findAll(clusterDatabaseKey)
        println("testing: ${data.size}")
    }
}