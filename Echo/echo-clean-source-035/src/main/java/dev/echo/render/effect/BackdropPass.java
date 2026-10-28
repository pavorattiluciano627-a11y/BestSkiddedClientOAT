package dev.echo.render.effect;

import dev.echo.client.render.BackdropMode;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.texture.TextureView;

/** GPU backdrop stage after Minecraft framebuffer acquisition and wrapping. */
public interface BackdropPass extends AutoCloseable {
    default BackdropTextures render(CommandEncoder commands, TextureView input,
                                    boolean sourceYUp, float radius) {
        return render(commands, input, sourceYUp, radius, BackdropMode.FULL);
    }

    BackdropTextures render(CommandEncoder commands, TextureView input,
                            boolean sourceYUp, float radius, BackdropMode mode);

    @Override
    void close();
}
