package org.cairnserver.util;

import org.slf4j.LoggerFactory;

//This is not for plugins
public final class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Logger.class);

    private Logger() {}

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void info(String format, Object... args) {
        LOGGER.info(format, args);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(format, args);
    }

    public static void error(String msg) {
        LOGGER.error(msg);
    }

    public static void error(String format, Object... args) {
        LOGGER.error(format, args);
    }

    public static void debug(String msg) {
        LOGGER.debug(msg);
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    public static void trace(String msg) {
        LOGGER.trace(msg);
    }

    public static void trace(String format, Object... args) {
        LOGGER.trace(format, args);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}