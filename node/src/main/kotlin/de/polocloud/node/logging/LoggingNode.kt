package de.polocloud.node.logging

import de.polocloud.node.core.environment.NodeEnvironment
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.*
import org.apache.logging.log4j.core.layout.PatternLayout

/**
 * Routes log events through the node's [de.polocloud.node.terminal.CliTerminal] instead of
 * writing straight to `System.out`. Writing to `System.out` directly would race with JLine's
 * line-editing buffer and corrupt the interactive prompt whenever a log message arrives while
 * the user is typing a command.
 *
 * Before the node's runtime/terminal is fully initialized (e.g. during early bootstrap), this
 * falls back to a plain [print] since there's no terminal to route through yet.
 */
@Plugin(name = "LoggingNode", category = "Core", elementType = "appender")
class LoggingNode(
    name: String,
    filter: Filter?,
    layout: Layout<*>,
) : AbstractAppender(name, filter, layout, true, null) {

    override fun append(event: LogEvent) {
        val formatted = layout.toSerializable(event).toString()

        try {
            NodeEnvironment.instance.context.cli.displayApproved(formatted)
        } catch (_: UninitializedPropertyAccessException) {
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
            return LoggingNode(name, null, layoutUsed)
        }
    }
}
