package dev.httpmarco.polocloud.modules.rest

import dev.httpmarco.polocloud.modules.rest.auth.JwtProvider
import dev.httpmarco.polocloud.modules.rest.auth.user.UserProvider
import dev.httpmarco.polocloud.modules.rest.config.RestConfiguration
import dev.httpmarco.polocloud.modules.rest.config.ConfigProvider
import dev.httpmarco.polocloud.modules.rest.config.Users
import dev.httpmarco.polocloud.modules.rest.controller.ControllerProvider
import dev.httpmarco.polocloud.shared.module.PolocloudModule
import dev.httpmarco.polocloud.shared.logging.Logger
import dev.httpmarco.polocloud.shared.polocloudShared
import java.nio.file.Files
import java.nio.file.Path

val logger: Logger = polocloudShared.logger()
val config: RestConfiguration = ConfigProvider().read("local/modules/rest/config", RestConfiguration())
val usersConfiguration: Users = ConfigProvider().read("local/modules/rest/users", Users())

class RestModule : PolocloudModule {

    private val configPath = Path.of("local/modules/rest")

    companion object {
        lateinit var instance: RestModule
            private set
    }

    lateinit var httpServer: HttpServer
        private set

    lateinit var jwtProvider: JwtProvider
        private set

    lateinit var controllerProvider: ControllerProvider
        private set

    lateinit var userProvider: UserProvider
        private set

    init {
        if (Files.notExists(this.configPath)) {
            Files.createDirectories(this.configPath)
        }
    }

    override fun onEnable() {
        instance = this

        this.httpServer = HttpServer()
        this.httpServer.start()

        this.jwtProvider = JwtProvider(usersConfiguration.secret)
        this.controllerProvider = ControllerProvider()

        this.userProvider = UserProvider()
        logger.info("Rest module started.")
    }

    override fun onDisable() {
        this.httpServer.stop()
        logger.info("Rest module stopped.")
    }
}