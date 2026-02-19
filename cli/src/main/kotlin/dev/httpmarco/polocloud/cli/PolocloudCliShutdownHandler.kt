package dev.httpmarco.polocloud.cli

import org.jline.jansi.AnsiConsole
import kotlin.system.exitProcess

private const val SHUTDOWN_HOOK = "polocloud-cli-shutdown-hook"
private var inShutdown = false

fun registerHook() {
    Runtime.getRuntime().addShutdownHook(Thread({
        exitPolocloud(cleanShutdown = false)
    }, SHUTDOWN_HOOK))
}

fun exitPolocloud(cleanShutdown: Boolean = true, shouldUpdate: Boolean = false) {

    if (inShutdown) {
        return
    }

    inShutdown = true

    //TODO translation

    //AnsiConsole.systemUninstall() TODO does currently not work

//    if (shouldUpdate) {
//        Updater.update()
//    }

    if (Thread.currentThread().name != SHUTDOWN_HOOK) {
        exitProcess(0)
    }
}

fun shutdownProcess(): Boolean {
    return inShutdown
}