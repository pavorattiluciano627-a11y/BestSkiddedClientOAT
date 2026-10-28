package dev.echo.render.queue;

import dev.echo.render.command.CommandEncoder;
import dev.echo.render.resource.GpuResource;

/** Queue capabilities use the renderer mask, not Vulkan's native queue flags. */
public abstract class CommandQueue extends GpuResource {
    public abstract int capabilities();
    public abstract CommandEncoder beginCommands();
    public abstract void submit(CommandEncoder commands);
    public abstract void waitIdle();
}
