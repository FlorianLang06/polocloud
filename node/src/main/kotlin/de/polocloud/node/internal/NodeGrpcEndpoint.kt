package de.polocloud.node.internal

import de.polocloud.common.Address
import de.polocloud.common.grpc.GrpcEndpoint
import de.polocloud.node.configuration.GeneralConfiguration
import de.polocloud.node.security.CertificateDataStorage
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth

class NodeGrpcEndpoint(address: Address, certificateDataStorage: CertificateDataStorage) {

    private val server = GrpcEndpoint.Builder(address)
        .tls(
            certificateDataStorage.caCertificateFile(),
            certificateDataStorage.certificateFile(),
            certificateDataStorage.privateKeyFile(),
            ClientAuth.REQUIRE
        )
        .build()

    fun start() {
        this.server.start()
    }
}