package de.polocloud.node.registration

import de.polocloud.common.Address
import de.polocloud.common.ShutdownMode
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.common.utils.localIpAddress
import de.polocloud.node.security.SecurityCertificateData
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import java.nio.file.Path

class RegistrationServer(address: Address) {

    private val certificateData = SecurityCertificateData(Path.of("testing"))
    private val grpcServer = GrpcEndpoint.Builder(address)
        .service(RegistrationService())
        .build()

    fun allowRequests() {
        println("Server started, listening on ${localIpAddress()}")
        this.grpcServer.start()
    }

    fun closeRequestServer() {
        // maybe a possible reason for a not clean close
        this.grpcServer.close(ShutdownMode.GRACEFUL)
    }
}