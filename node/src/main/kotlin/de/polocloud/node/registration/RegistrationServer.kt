package de.polocloud.node.registration

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.common.utils.localIpAddress
import de.polocloud.node.repositories.NodeRepository
import de.polocloud.node.shutdown.ShutdownHook
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import java.nio.file.Path

class RegistrationServer(registrationManager: RegistrationManager, address: Address, nodeRepository: NodeRepository) : Closeable {

    private val grpcServer = GrpcEndpoint.Builder(address)
        .service(RegistrationService(registrationManager, nodeRepository))
        .build()

    fun allowRequests() {
        this.grpcServer.start()

        ShutdownHook.attach(this)
    }

    override fun close(mode: ShutdownMode) {
        // maybe a possible reason for a not clean close
        this.grpcServer.close(ShutdownMode.GRACEFUL)
    }
}