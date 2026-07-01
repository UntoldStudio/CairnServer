package org.cairnserver.util;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "LoggerSourceConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"cairnSource"})
public class LoggerSourceConverter extends LogEventPatternConverter {
    protected LoggerSourceConverter(String name, String style) {
        super(name, style);
    }

    public static LoggerSourceConverter newInstance(final String[] options) {
        return new LoggerSourceConverter("cairnSource", "cairnSource");
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        String loggerName = event.getLoggerName();
        if (loggerName.startsWith("plugin.")) {
            toAppendTo.append(loggerName.substring("plugin.".length()));
        } else if (loggerName.startsWith("org.cairnserver")) {
            toAppendTo.append("Cairn");
        } else if (loggerName.startsWith("net.minecraft")) {
            toAppendTo.append("Minecraft");
        } else {
            toAppendTo.append("Unknown");
        }
    }
}
