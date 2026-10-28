package dev.echo.render.effect;

import dev.echo.render.texture.TextureView;

/** Borrowed views of effect-owned outputs; callers do not release either view. */
public record BackdropTextures(TextureView halfResolution, TextureView fullResolution) {
}
