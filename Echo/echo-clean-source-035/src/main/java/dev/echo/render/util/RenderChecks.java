package dev.echo.render.util;

import org.slf4j.helpers.MessageFormatter;

/** Pending direct target proof for the original custom identity catch. */
public final class RenderChecks {
    public static void require(boolean condition, String message, Object... arguments) {
        if (!condition) {
            throw new RenderException(MessageFormatter.arrayFormat(message, arguments).getMessage());
        }
    }

    private RenderChecks() {}
}
