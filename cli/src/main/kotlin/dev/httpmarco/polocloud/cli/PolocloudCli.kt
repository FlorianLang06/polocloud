package dev.httpmarco.polocloud.cli

import dev.httpmarco.polocloud.cli.jline.JLine3Terminal
import dev.httpmarco.polocloud.cli.logging.LoggingCli
import dev.httpmarco.polocloud.cli.logging.LoggingLayout
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.LoggerContext

var logger = initLogging()

object PolocloudCli {

    fun start() {
        val terminal: JLine3Terminal = JLine3Terminal()
        terminal.jLine3Reading.start()

    }

}

fun initLogging(debugMode: Boolean = false): Logger {
    val ctx = LoggerContext.getContext(false)
    val config = ctx.configuration
    val rootLoggerConfig = config.rootLogger

    val existingAppenderList = ArrayList(rootLoggerConfig.appenders.values)
    existingAppenderList.forEach { appender ->
        appender.stop()
        rootLoggerConfig.removeAppender(appender.name)
        config.appenders.remove(appender.name)
    }

    rootLoggerConfig.isAdditive = false

    val layout = LoggingLayout.createLayout()
    val appender = LoggingCli.create("LoggingAgent", layout)
    appender.start()
    config.addAppender(appender)

    rootLoggerConfig.level = if (debugMode) Level.DEBUG else Level.INFO
    rootLoggerConfig.addAppender(appender, rootLoggerConfig.level, null)

    ctx.updateLoggers()
    return LogManager.getLogger("PoloCloud")
}