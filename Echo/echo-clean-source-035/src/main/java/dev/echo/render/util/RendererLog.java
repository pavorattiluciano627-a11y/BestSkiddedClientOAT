package dev.echo.render.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared renderer logger; the logger category is an existing compatibility ID. */
public final class RendererLog {
    public static final Logger LOGGER = LoggerFactory.getLogger("chimera");

    private RendererLog() {
    }
}
