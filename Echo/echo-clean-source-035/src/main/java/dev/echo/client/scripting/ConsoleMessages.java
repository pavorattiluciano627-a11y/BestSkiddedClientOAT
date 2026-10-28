package dev.echo.client.scripting;

import org.mozilla.javascript.Context;
import org.slf4j.Logger;

/** Console message operations. The caller supplies the script engine's real logger. */
public final class ConsoleMessages {
    private ConsoleMessages() {
    }

    public static void info(Logger logger, Object[] arguments) {
        logger.info("{}", format(arguments));
    }

    public static void warn(Logger logger, Object[] arguments) {
        logger.warn("{}", format(arguments));
    }

    public static void error(Logger logger, Object[] arguments) {
        logger.error("{}", format(arguments));
    }

    public static void debug(Logger logger, Object[] arguments) {
        logger.debug("{}", format(arguments));
    }

    /** Converts in array order, propagating exceptions from JavaScript toString. */
    public static String format(Object[] arguments) {
        if (arguments.length == 0) {
            return "";
        }
        StringBuilder message = new StringBuilder();
        for (int index = 0; index < arguments.length; index++) {
            if (index > 0) {
                message.append(' ');
            }
            message.append(Context.toString(arguments[index]));
        }
        return message.toString();
    }
}
