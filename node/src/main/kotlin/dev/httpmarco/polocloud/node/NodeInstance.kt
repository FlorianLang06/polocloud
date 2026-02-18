package dev.httpmarco.polocloud.node

import dev.httpmarco.polocloud.common.configuration.ConfigSection
import dev.httpmarco.polocloud.common.grpc.GrpcEndpoint
import dev.httpmarco.polocloud.common.utils.TerminalUtils
import dev.httpmarco.polocloud.database.DatabaseCredentials
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.i18n.model.Language
import dev.httpmarco.polocloud.node.configuration.NodeInstanceConfiguration

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

        // connect GRPC endpoint
        endpoint.connect()

        TranslationService.init()
        TranslationService.defaultLanguage("en_US")
        // TODO get language from config and ask the user on setup with e.g. TranslationService#defaultLanguage("database")
        TranslationService.preloadAsync("database", Language("en_US"))
        //TODO preLoad all translation packs e.g. database when cluster is enabled and db is needed
    }

    fun generateConfiguration(): NodeInstanceConfiguration {
        return ConfigSection(LOCAL_NODE_PATH).readOrCreate(NodeInstanceConfiguration.serializer(), NodeInstanceConfiguration())
    }
}
