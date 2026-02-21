package dev.httpmarco.polocloud.cli.command.impl

import dev.httpmarco.polocloud.cli.command.Command
import dev.httpmarco.polocloud.cli.exitPolocloud

class ShutdownCommand : Command("shutdown", "Shuts down the agent", "stop") {

    init {
        defaultExecution {
            exitPolocloud()
        }
    }

}