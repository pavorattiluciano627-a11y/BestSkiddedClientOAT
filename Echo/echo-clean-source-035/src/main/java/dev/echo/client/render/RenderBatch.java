package dev.echo.client.render;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;

/** Proposed canonical client batch contract, keyed by original virtual slots in symbols.json. */
public interface RenderBatch {
    /** The original interface declares this optional preparation hook as a no-op. */
    default void prepare(CommandEncoder encoder) {}

    /** The original interface declares this optional frame reset hook as a no-op. */
    default void reset() {}

    void draw(CommandEncoder encoder, RenderPass pass);

    void destroy();
}
