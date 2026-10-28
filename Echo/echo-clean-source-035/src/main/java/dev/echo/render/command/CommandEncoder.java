package dev.echo.render.command;

import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.surface.RenderTargetLease;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

public abstract class CommandEncoder {
    public abstract TransferPass beginTransfer();
    public abstract ComputePass beginComputePass();
    public abstract RenderPass beginExternalRenderPass(Supplier<RenderTargetLease> target);
    public abstract RenderPass beginRenderPass(TextureView color, OptionalInt clearColor,
                                              TextureView depth, OptionalDouble clearDepth);
    public abstract void transitionBuffer(BufferSlice slice, int stages, BufferAccess access);
    public abstract void transitionTexture(TextureView view, int stages, TextureAccess access, TextureLayout layout);
}
