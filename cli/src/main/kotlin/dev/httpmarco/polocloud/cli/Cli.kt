package dev.httpmarco.polocloud.cli

import dev.httpmarco.polocloud.cli.logging.CliLogger
import dev.httpmarco.polocloud.cli.terminal.CliTerminal
import dev.httpmarco.polocloud.i18n.api.TranslationService

/**
 * Application-wide logger instance, initialized once at startup.
 */
var logger = CliLogger.initLogging()

/**
 * Entry point and lifecycle manager for the PoloCloud CLI application.
 *
 * Responsible for initializing the terminal, loading translations,
 * starting the command reading loop, and handling graceful shutdown.
 */
object PolocloudCli {
    val terminal: CliTerminal = CliTerminal()

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger.fatal("Uncaught exception in thread '${thread.name}'", throwable)
        }
    }

    /**
     * Starts the CLI application.
     *
     * Clears the screen, loads translations, and begins the command reading loop.
     */
    fun start() {
        this.terminal.clearScreen()

        TranslationService.init()
        TranslationService.defaultLanguage("en_US") // TODO get local from installer config
        TranslationService.preloadAsync("cli")
        logger.info(TranslationService.tr("cli", "cli.start.initiating", "version" to "")) //TODO show version

        this.terminal.readingThread.start()
        logger.info(TranslationService.tr("cli", "cli.start.success"))
    }

    /**
     * Stops the CLI application and shuts down the terminal.
     */
    fun stop() {
        this.terminal.shutdown()
    }

}