package dev.httpmarco.polocloud.cli

import dev.httpmarco.polocloud.cli.jline.JLine3Terminal
import dev.httpmarco.polocloud.cli.logging.PolocloudLogger
import dev.httpmarco.polocloud.i18n.api.TranslationService

var logger = PolocloudLogger.initLogging()

object PolocloudCli {
    val terminal: JLine3Terminal = JLine3Terminal()

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logger.fatal("Uncaught exception in thread '${thread.name}'", throwable)
        }
    }

    fun start() {
        TranslationService.init()
        TranslationService.defaultLanguage("en_US") // TODO get local from config
        TranslationService.preloadAsync("cli")
        logger.info(TranslationService.tr("cli", "cli.start.initiating", "version" to "")) //TODO show version

        this.terminal.jLine3Reading.start()
        logger.info(TranslationService.tr("cli", "cli.start.success"))
    }

}