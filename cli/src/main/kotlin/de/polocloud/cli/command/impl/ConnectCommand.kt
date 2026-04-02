package de.polocloud.cli.command.impl

import de.polocloud.cli.Cli
import de.polocloud.cli.command.Command
import de.polocloud.cli.command.arguments.type.int.IntArgument
import de.polocloud.cli.command.arguments.type.string.TextArgument
import de.polocloud.cli.connection.ClusterConnection
import de.polocloud.cli.logger
import de.polocloud.common.Address
import de.polocloud.common.error.extensions.getOrReport

class ConnectCommand : Command("connect", "cli.command.impl.connect.description") {

    private val hostArg = TextArgument("host")
    private val portArg = IntArgument("port", minValue = 1, maxValue = 65535)
    private val tokenArg = TextArgument("token")

    init {
        // connect <host> <port> <token>  — erstmalige Registrierung
        syntax(
            { ctx ->
                val address = Address(ctx.arg(hostArg), ctx.arg(portArg))
                val conn = ClusterConnection(address, Cli.certificateStorage)

                val result = conn.register(ctx.arg(tokenArg))

                if (result.isSuccess) {
                    logger.info("Registration successful — connecting...")
                    Cli.connectToCluster(address)
                } else {
                    result.getOrReport()
                }
            },
            "Register and connect to a cluster node",
            hostArg, portArg, tokenArg
        )

        // connect <host> <port>  — mit vorhandenem Cert
        syntax(
            { ctx ->
                val address = Address(ctx.arg(hostArg), ctx.arg(portArg))
                Cli.connectToCluster(address)
            },
            "Connect using existing certificate",
            hostArg, portArg
        )
    }
}