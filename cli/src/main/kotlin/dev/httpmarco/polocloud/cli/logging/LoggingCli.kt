package dev.httpmarco.polocloud.cli.logging

import dev.httpmarco.polocloud.cli.PolocloudCli
import dev.httpmarco.polocloud.cli.shutdownProcess
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.*
import org.apache.logging.log4j.core.layout.PatternLayout

@Plugin(name = "LoggingCli", category = "Core", elementType = "appender")
class LoggingCli(
    name: String,
    filter: Filter?,
    layout: Layout<*>,
) : AbstractAppender(name, filter, layout, true, null) {

    override fun append(event: LogEvent) {
        val formatted = layout.toSerializable(event).toString()

        if (!shutdownProcess()) {
          //  PolocloudCli.terminal.displayApproved(formatted)
        } else {
            print(formatted)
        }
    }

    companion object {
        @PluginFactory
        @JvmStatic
        fun create(
            @PluginAttribute("name") name: String,
            @PluginElement("Layout") layout: Layout<*>?,
        ): Appender {
            val layoutUsed = layout ?: PatternLayout.createDefaultLayout()
            return LoggingCli(name, null, layoutUsed)
        }
    }
}