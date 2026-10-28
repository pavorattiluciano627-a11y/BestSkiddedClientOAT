package dev.echo.render.surface;

/** Scoped access to an externally owned render target. */
public interface RenderTargetLease extends AutoCloseable {
    int width();

    int height();

    /** The original interface supplies an unflipped default. */
    default boolean isYFlipped() {
        return false;
    }

    /** Releases the scope, without implying ownership of the target texture. */
    @Override
    void close();
}
