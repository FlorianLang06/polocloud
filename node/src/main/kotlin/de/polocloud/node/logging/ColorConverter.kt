package de.polocloud.node.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import org.apache.logging.log4j.core.pattern.PatternFormatter

@Plugin(name = "ColorConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("color")
class ColorConverter private constructor(
    private val formatters: List<PatternFormatter>,
    private val color: AnsiColor?
) : LogEventPatternConverter("color", "color") {

    override fun format(event: LogEvent, toAppendTo: StringBuilder) {

        val start = toAppendTo.length

        for (formatter in formatters) {
            formatter.format(event, toAppendTo)
        }

        val text = toAppendTo.substring(start)
        toAppendTo.setLength(start)

        val resolvedColor = color ?: when (event.level) {
            Level.ERROR -> AnsiColor.RED
            Level.WARN -> AnsiColor.YELLOW
            Level.INFO -> AnsiColor.BRIGHT_WHITE
            Level.DEBUG -> AnsiColor.CYAN
            Level.TRACE -> AnsiColor.WHITE
            else -> AnsiColor.WHITE
        }

        toAppendTo.append(resolvedColor.colorize(text))
    }

    companion object {

        @JvmStatic
        fun newInstance(options: Array<String>?): ColorConverter {

            val pattern = options?.getOrNull(0) ?: "%msg"
            val colorName = options?.getOrNull(1)

            val parser = PatternLayout.createPatternParser(null)
            val formatters = parser.parse(pattern)

            val color = AnsiColor.fromName(colorName)

            return ColorConverter(formatters, color)
        }
    }
}