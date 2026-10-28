package dev.echo.render.surface;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.queue.CommandQueue;
import dev.echo.render.resource.GpuResource;
import dev.echo.render.texture.TextureView;

/** Window presentation target. The status slot reports whether a resize is needed. */
public abstract class Surface extends GpuResource {
    public abstract void resize(int width, int height, PresentMode presentMode);
    public abstract boolean needsResize();
    public abstract TextureView acquireTexture();
    public abstract void present(CommandQueue queue, CommandEncoder commands);
}
