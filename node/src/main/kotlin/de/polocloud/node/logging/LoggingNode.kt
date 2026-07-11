package de.polocloud.node.logging

import de.polocloud.node.core.environment.NodeEnvironment
import de.polocloud.node.terminal.AnsiColors
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
 * When there's no terminal to route through — either before the runtime/terminal is fully
 * initialized during early bootstrap, or after it has already been closed during shutdown —
 * this falls back to a plain [print]. The appender must never propagate an exception back
 * into log4j, otherwise log4j reports it as "An exception occurred processing Appender".
 */
@Plugin(name = "LoggingNode", category = "Core", elementType = "appender")
class LoggingNode(
    name: String,
    filter: Filter?,
    layout: Layout<*>,
) : AbstractAppender(name, filter, layout, true, null) {

    override fun append(event: LogEvent) {
        val raw = layout.toSerializable(event).toString()

        // Translate legacy `&x` colour codes (used in command help and command output)
        // into ANSI here, on the console path only — the file appender keeps the raw text.
        // AnsiColors touches org.jline.jansi classes on first use; during early bootstrap
        // those may still be downloading on another thread, which permanently breaks the
        // enum's static init (NoClassDefFoundError). Fall back to the raw, uncoloured text
        // rather than let that escape and take the whole appender down with it.
        val formatted = try {
            AnsiColors.translate(raw)
        } catch (_: Throwable) {
            raw
        }

        try {
            NodeEnvironment.instance.context.cli.displayApproved(formatted)
        } catch (_: Exception) {
            // No usable terminal: not yet initialized (early bootstrap) or already
            // closed (shutdown, when JLine tears the terminal down). Fall back to
            // stdout so the message survives and the appender never throws back.
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
