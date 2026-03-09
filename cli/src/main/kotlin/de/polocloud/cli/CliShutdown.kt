package de.polocloud.cli

import de.polocloud.i18n.api.TranslationService
import org.jline.jansi.AnsiConsole
import kotlin.system.exitProcess

private const val SHUTDOWN_HOOK = "polocloud-cli-shutdown-hook"
private var inShutdown = false

fun registerHook() {
    Runtime.getRuntime().addShutdownHook(Thread({
        exitPolocloud(cleanShutdown = false)
    }, SHUTDOWN_HOOK))
}

fun exitPolocloud(cleanShutdown: Boolean = true) {
    if (inShutdown) {
        logger.warn(TranslationService.tr("cli", "cli.shutdown.already_in_progress"))
        return
    }

    inShutdown = true
    logger.info(TranslationService.tr("cli", "cli.shutdown.initiating"))

    //TODO some shutdown logic between the two translations
    PolocloudCli.stop()

    val key = if (cleanShutdown) {
        "cli.shutdown.clean"
    } else {
        "cli.shutdown.forced"
    }

    logger.info(TranslationService.tr("cli", key))

    if (AnsiConsole.isInstalled()) {
        AnsiConsole.systemUninstall()
    }


    if (Thread.currentThread().name != SHUTDOWN_HOOK) {
        exitProcess(0)
    }
}

fun shutdownProcess(): Boolean {
    return inShutdown
}