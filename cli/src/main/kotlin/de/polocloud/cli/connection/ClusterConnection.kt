package de.polocloud.cli.connection

import de.polocloud.cli.error.CliError
import de.polocloud.common.Address
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.error.exception.PoloException
import de.polocloud.common.error.exception.PoloResult
import de.polocloud.common.error.extensions.asSuccess
import de.polocloud.common.generator.CertificateSigningRequestGenerator
import de.polocloud.proto.CliRegistrationServiceGrpcKt
import de.polocloud.proto.ClusterQueryServiceGrpcKt
import de.polocloud.proto.RegisterCliRequest
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.runBlocking
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.TimeUnit

class ClusterConnection(
    private val address: Address,
    private val storage: CliCertificateStorage,
) : Closeable {

    private var channel: ManagedChannel? = null

    var queryStub: ClusterQueryServiceGrpcKt.ClusterQueryServiceCoroutineStub? = null
        private set

    fun connect() {
        val sslContext = GrpcSslContexts.forClient()
            .keyManager(storage.certificateFile(), storage.privateKeyFile())
            .trustManager(storage.caCertificateFile())
            .build()

        channel = NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .sslContext(sslContext)
            .build()

        queryStub = ClusterQueryServiceGrpcKt.ClusterQueryServiceCoroutineStub(channel!!)
    }

    fun register(token: String): PoloResult<Unit> {
        val csr = CertificateSigningRequestGenerator(storage.keyPair, UUID.randomUUID()).generate()
        val csrPem = StringWriter().also { JcaPEMWriter(it).use { w -> w.writeObject(csr) } }.toString()

        val tempChannel = NettyChannelBuilder
            .forAddress(address.hostname, address.port)
            .usePlaintext()
            .build()

        return runCatching {
            val stub = CliRegistrationServiceGrpcKt.CliRegistrationServiceCoroutineStub(tempChannel)

            val response = runBlocking {
                stub.registerCli(
                    RegisterCliRequest.newBuilder()
                        .setToken(token)
                        .setCsrPem(csrPem)
                        .build()
                )
            }

            if (!response.accepted) {
                return CliError.RegistrationDenied(response.message).asFailure()
            }

            storage.saveCertificate(response.certificatePem)
            storage.saveCaCertificate(response.caCertificatePem)

            Unit.asSuccess()
        }.getOrElse {
            throw PoloException(CliError.RegistrationFailed(address = "${address.hostname}:${address.port}", causeMsg = it.message ?: "unknown"))
        }.also {
            tempChannel.shutdown()
            tempChannel.awaitTermination(3, TimeUnit.SECONDS)
        }
    }

    override fun close(mode: ShutdownMode) {
        channel?.shutdown()
    }
}