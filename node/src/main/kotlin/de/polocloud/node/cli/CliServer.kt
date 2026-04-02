package de.polocloud.node.cli

import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.node.NodeInstance
import de.polocloud.node.configuration.cluster.CliAccessConfiguration
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.security.CertificateDataStorage
import de.polocloud.node.shutdown.ShutdownHook
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import org.slf4j.LoggerFactory
import java.security.KeyPair

class CliServer(
    private val config: CliAccessConfiguration,
    private val nodeRepository: NodeRepository,
    private val keyPair: KeyPair,
    private val certificateDataStorage: CertificateDataStorage,
) : Closeable {

    private val logger = LoggerFactory.getLogger(javaClass)
    private var server: Server? = null

    fun start() {
        if (!config.enabled) {
            logger.info("CLI access is disabled in cluster.json")
            return
        }

        if (config.allowedIps.isEmpty()) {
            logger.warn("CLI access enabled but allowedIps is empty — no client can connect")
        }

        val interceptor = IpWhitelistInterceptor(config.allowedIps)

        server = NettyServerBuilder
            .forPort(config.port)
            // Registrierung läuft plaintext — Cert existiert beim CLI noch nicht
            .addService(
                ServerInterceptors.intercept(
                CliRegistrationService(config, keyPair),
                interceptor
            ))
            // Query läuft mit mTLS — CLI muss sich mit Cert ausweisen
            .addService(ServerInterceptors.intercept(
                ClusterQueryService(nodeRepository),
                interceptor
            ))
            .useTransportSecurity(
                certificateDataStorage.certificateFile(),
                certificateDataStorage.privateKeyFile()
            )
            .build()
            .start()

        logger.info("CLI server started on port ${config.port}")
        ShutdownHook.attach(this)
    }

    override fun close(mode: ShutdownMode) {
        server?.shutdown()
    }
}