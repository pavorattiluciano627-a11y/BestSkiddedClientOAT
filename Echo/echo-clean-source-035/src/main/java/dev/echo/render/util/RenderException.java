package dev.echo.render.util;

/** Renderer invariant failure. */
public class RenderException extends RuntimeException {
    public RenderException(String message) {
        super(String.format("Validation error : %s", message));
    }
}
