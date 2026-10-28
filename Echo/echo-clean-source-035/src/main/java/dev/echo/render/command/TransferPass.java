package dev.echo.render.command;

import dev.echo.render.buffer.BufferSlice;
import dev.echo.render.texture.TextureView;

/** Upload commands; end completes the pass without taking ownership of its resources. */
public abstract class TransferPass {
    public abstract void uploadTexture(TextureView destination, BufferSlice source);
    public abstract void end();
}
