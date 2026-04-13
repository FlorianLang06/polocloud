package de.polocloud.cli

import de.polocloud.cli.CliPaths.CONFIG_FILE
import de.polocloud.cli.CliPaths.INSTALLER_FILE
import de.polocloud.cli.configuration.CliConfiguration
import de.polocloud.cli.configuration.InstallerConfig
import de.polocloud.cli.configuration.connection.ConnectionHistory
import de.polocloud.cli.connection.CliCertificateStorage
import de.polocloud.cli.connection.CliConnectionManager
import de.polocloud.cli.connection.auto.AutoConnectService
import de.polocloud.cli.logging.CliLogger
import de.polocloud.cli.terminal.CliTerminal
import de.polocloud.common.Closeable
import de.polocloud.common.ShutdownMode
import de.polocloud.common.configuration.ConfigurationManager
import de.polocloud.common.version.PolocloudVersion
import de.polocloud.i18n.api.TranslationService
import de.polocloud.i18n.model.Language

/**
 * Application-wide logger instance, initialized once at startup.
 */
var logger = CliLogger.initLogging(PolocloudVersion.CURRENT.isDebugEnabled)

/**
 * Entry point and lifecycle manager for the PoloCloud CLI application.
 *
 * Responsible for initializing the terminal, loading translations,
 * starting the command reading loop, and handling graceful shutdown.
 */
object Cli : Closeable {
    val config: CliConfiguration = loadConfiguration()

    private val certificateStorage = CliCertificateStorage()
    val connectionManager = CliConnectionManager(certificateStorage)
    val connectionHistory = ConnectionHistory(certificateStorage.keyPair)
    val lifecycle: AutoConnectService = AutoConnectService(connectionManager, connectionHistory)
    val terminal: CliTerminal = CliTerminal(connectionManager)

    init {
        TranslationService.init()
        TranslationService.defaultLanguage(config.locale)
        TranslationService.preloadAsync("errors")
        TranslationService.preloadAsync("cli")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger.fatal("Uncaught exception in thread '${thread.name}'", throwable)
        }
    }

    //TODO global Updater for all modules (cli, node)

    /**
     * Starts the CLI application.
     *
     * Clears the screen, loads translations, and begins the command reading loop.
     */
    fun start() {
        this.terminal.clearScreen()

        logger.info(TranslationService.tr("cli", "cli.start.initiating", "version" to PolocloudVersion.CURRENT.toDisplayString()))
        this.lifecycle.start()
        this.terminal.readingThread.start()

        logger.info(TranslationService.tr("cli", "cli.start.success"))
    }

    /**
     * Stops the CLI application and shuts down the terminal.
     */
    override fun close(mode: ShutdownMode) {
        lifecycle.stop()
        this.terminal.shutdown()
    }

    /**
     * Resolves the CLI runtime configuration.
     *
     * The configuration is loaded from the primary CLI configuration source
     * (e.g. {@code polocloud-cli.json}). If no configuration exists yet,
     * a new one will be created automatically using an initial bootstrap configuration.
     *
     * The bootstrap configuration may derive values from the installer configuration
     * (if available). This allows transferring selected setup parameters
     * such as the language into the runtime configuration during first startup.
     *
     * Once the CLI configuration has been created, it becomes the single
     * source of truth and the installer configuration is no longer consulted.
     *
     * @return the resolved {@link CliConfiguration}, never {@code null}
     */
    private fun loadConfiguration(): CliConfiguration {
        val path = CONFIG_FILE.toPath()

        val holder = ConfigurationManager
            .load<CliConfiguration>()

        return if (path.toFile().exists()) {
            holder.value
        } else {
            val initial = resolveInitialConfiguration()
            holder.value = initial
            initial
        }
    }

    /**
     * Determines the initial CLI configuration used during first-time startup.
     *
     * If an installer configuration is present, relevant values (e.g. language)
     * are transferred into the initial CLI configuration.
     *
     * If no installer configuration is available, a default configuration is returned.
     *
     * This method is only relevant when no persistent CLI configuration exists yet.
     */
    private fun resolveInitialConfiguration(): CliConfiguration {
        val installerPath = INSTALLER_FILE.toPath()
        val installer = ConfigurationManager.read(installerPath,InstallerConfig.serializer())

        return installer?.let {
            CliConfiguration(locale = Language.of(it.language))
        } ?: CliConfiguration()
    }


}