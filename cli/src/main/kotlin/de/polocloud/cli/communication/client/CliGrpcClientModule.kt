package de.polocloud.cli.communication.client

import de.polocloud.cli.communication.middleware.ClientErrorMiddleware
import de.polocloud.cli.communication.middleware.ClientLoggingMiddleware
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.common.communication.client.executor.GrpcClientExecutor

object CliGrpcClientModule {

    fun createExecutor(connectionManager: CliConnectionManager): GrpcClientExecutor {
        return GrpcClientExecutor(
            channelProvider = { connectionManager.channel() },
            middlewares = listOf(
                ClientErrorMiddleware(),
                ClientLoggingMiddleware()
            )
        )
    }
}