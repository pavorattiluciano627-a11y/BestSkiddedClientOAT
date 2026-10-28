package dev.echo.client.render;

import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.Texture;

/** Owned gradient, horizontal blur and vertical blur targets for one material-pass size. */
public record TexturePassResources(Texture outline, Texture horizontalBlur, Texture verticalBlur) {
    public void release(RenderDevice device) {
        device.release(outline);
        device.release(horizontalBlur);
        device.release(verticalBlur);
    }
}
