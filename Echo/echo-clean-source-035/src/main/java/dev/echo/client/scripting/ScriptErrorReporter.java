package dev.echo.client.scripting;

import org.mozilla.javascript.RhinoException;
import org.slf4j.Logger;

/** Script diagnostics using the engine logger and Rhino's original source metadata. */
public final class ScriptErrorReporter {
    private ScriptErrorReporter() {
    }

    public static void report(Logger logger, String scriptId, String message) {
        logger.error("[{}] {}", scriptId, message);
    }

    public static void report(Logger logger, String scriptId, String operation, Throwable error) {
        if (error instanceof RhinoException scriptError) {
            logger.error("[{}] {}: {} ({}:{})", scriptId, operation,
                scriptError.details(), scriptError.sourceName(), scriptError.lineNumber());
            return;
        }
        error.printStackTrace();
    }
}
