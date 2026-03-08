package de.polocloud.cli

import de.polocloud.cli.configuration.CliConfiguration
import de.polocloud.cli.configuration.InstallerConfig
import de.polocloud.cli.logging.CliLogger
import de.polocloud.cli.terminal.CliTerminal
import de.polocloud.common.configuration.ConfigSection
import de.polocloud.common.version.PolocloudVersion
import dev.httpmarco.polocloud.i18n.api.TranslationService
import dev.httpmarco.polocloud.i18n.model.Language
import java.io.File

/**
 * Application-wide logger instance, initialized once at startup.
 */
var logger = _root_ide_package_.de.polocloud.cli.logging.CliLogger.initLogging()

/**
 * Entry point and lifecycle manager for the PoloCloud CLI application.
 *
 * Responsible for initializing the terminal, loading translations,
 * starting the command reading loop, and handling graceful shutdown.
 */
object PolocloudCli {
    val config: de.polocloud.cli.configuration.CliConfiguration = loadConfiguration()
    val terminal: de.polocloud.cli.terminal.CliTerminal = _root_ide_package_.de.polocloud.cli.terminal.CliTerminal()

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            _root_ide_package_.de.polocloud.cli.logger.fatal("Uncaught exception in thread '${thread.name}'", throwable)
        }
    }

    //TODO making all dot folders invisible
    //TODO global Updater for all modules (cli, node)

    /**
     * Starts the CLI application.
     *
     * Clears the screen, loads translations, and begins the command reading loop.
     */
    fun start() {
        this.terminal.clearScreen()

        TranslationService.init()
        TranslationService.defaultLanguage(config.locale)
        TranslationService.preloadAsync("cli")
        _root_ide_package_.de.polocloud.cli.logger.info(TranslationService.tr("cli", "cli.start.initiating", "version" to PolocloudVersion.CURRENT.toDisplayString()))

        this.terminal.readingThread.start()
        _root_ide_package_.de.polocloud.cli.logger.info(TranslationService.tr("cli", "cli.start.success"))
    }

    /**
     * Stops the CLI application and shuts down the terminal.
     */
    fun stop() {
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
    private fun loadConfiguration(): de.polocloud.cli.configuration.CliConfiguration {
        val path = File("polocloud-cli.json").toPath()
        val section = ConfigSection(path)

        return section.readOrCreate(
            _root_ide_package_.de.polocloud.cli.configuration.CliConfiguration.serializer(),
            resolveInitialConfiguration()
        )
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
    private fun resolveInitialConfiguration(): de.polocloud.cli.configuration.CliConfiguration {
        val installerPath = File(".installer/config.json").toPath()

        val installer = ConfigSection(installerPath)
            .read(_root_ide_package_.de.polocloud.cli.configuration.InstallerConfig.serializer())

        return installer?.let {
            _root_ide_package_.de.polocloud.cli.configuration.CliConfiguration(locale = Language.of(it.language))
        } ?: _root_ide_package_.de.polocloud.cli.configuration.CliConfiguration()
    }


}