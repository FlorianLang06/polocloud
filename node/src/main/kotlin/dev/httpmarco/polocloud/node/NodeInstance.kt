package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.common.utils.TerminalUtils
import dev.httpmarco.polocloud.common.utils.toBytes
import dev.httpmarco.polocloud.common.utils.toUUID
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.i18n.model.Language
import dev.httpmarco.polocloud.node.cluster.Cluster
import dev.httpmarco.polocloud.node.configuration.NodeInstanceConfiguration
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

/**
 * Singleton representing the local PoloCloud node.
 *
 * Responsible for:
 * 1. Loading configuration
 * 2. Initializing GRPC endpoint
 * 3. Establishing database connection
 */
object NodeInstance {

    val config: NodeInstanceConfiguration by lazy { generateConfiguration() }

    /** GRPC endpoint for inter-node communication */
    val endpoint: GrpcEndpoint by lazy { GrpcEndpoint(config.bindAddress) }

    init {
        TerminalUtils.clear()

        TranslationService.init()
        TranslationService.defaultLanguage("en_US")
        // TODO get language from config and ask the user on setup with e.g. TranslationService#defaultLanguage("database")
        TranslationService.preloadAsync("database", Language("en_US"))
        TranslationService.preloadAsync("cluster", Language("en_US"))
        //TODO preLoad all translation packs e.g. database when cluster is enabled and db is needed

        // connect GRPC endpoint
        endpoint.connect()

        Cluster.detect()

        // TODO load required services (e.g. database) and ensure connectivity before marking the node as online

        // finally mark the node as online in the cluster
        Cluster.markOnline()
    }

    fun generateConfiguration(): NodeInstanceConfiguration {
        return ConfigSection(LOCAL_NODE_PATH).readOrCreate(
            NodeInstanceConfiguration.serializer(),
            NodeInstanceConfiguration()
        )
    }
}
