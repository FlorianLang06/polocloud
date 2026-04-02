package de.polocloud.node.internal

import de.polocloud.common.Address
import de.polocloud.node.security.CertificateDataStorage
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

class NodeGrpcClient(val certificateDataStorage: CertificateDataStorage) {

    private var channel : ManagedChannel? = null

    fun connect(address: Address) {
        channel = createChannel(address)
    }

    private fun createChannel(address: Address): ManagedChannel {
        val sslContext = GrpcSslContexts.forClient()
            .keyManager(
                certificateDataStorage.certificateFile(),
                certificateDataStorage.privateKeyFile()
            )
            .trustManager(
                certificateDataStorage.caCertificateFile()
            )
            .build()

        return NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .sslContext(sslContext)
            .build()
    }
}