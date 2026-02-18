package dev.httpmarco.polocloud.node.cluster

import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.node.NodeInstance
import org.slf4j.LoggerFactory

object Cluster {

    private val logger = LoggerFactory.getLogger(Cluster.javaClass)
    private val database = NodeInstance.config.database.factory()

    init {
        database.connect()

        logger.info(TranslationService.tr("cluster", "cluster.localId", NodeInstance.localId.toString()))
    }

    fun detect() {

    }
}