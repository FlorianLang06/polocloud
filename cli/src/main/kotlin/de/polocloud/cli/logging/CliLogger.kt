package de.polocloud.cli.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.LoggerContext

object CliLogger {
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
        val appender = LoggingCli.create("LoggingCli", layout)
        appender.start()
        config.addAppender(appender)

        rootLoggerConfig.level = if (debugMode) Level.DEBUG else Level.INFO
        rootLoggerConfig.addAppender(appender, rootLoggerConfig.level, null)

        ctx.updateLoggers()
        return LogManager.getLogger("PoloCloud CLI")
    }
}