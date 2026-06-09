package de.polocloud.node.communication.registration.server

import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.communication.GrpcEndpoint
import de.polocloud.node.communication.registration.cli.CliRegistrationService
import de.polocloud.node.communication.interceptor.IpWhitelistInterceptor
import de.polocloud.node.communication.registration.node.RegistrationManager
import de.polocloud.node.communication.registration.node.service.RegistrationService

class RegistrationServer(
    registrationManager: RegistrationManager,
    address: Address,
    cliRegistrationService: CliRegistrationService,
) : Closeable {

    private val grpcServer = GrpcEndpoint.Builder(address)
        .service(RegistrationService(registrationManager))
        .interceptedService(
            cliRegistrationService,
            IpWhitelistInterceptor()
        )
        .build()

    fun allowRequests() {
        this.grpcServer.start()
    }

    override fun close(mode: ShutdownMode) {
        // maybe a possible reason for a not clean close
        this.grpcServer.close(ShutdownMode.GRACEFUL)
    }
}