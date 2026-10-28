package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.TextureView;

/** Reference-identity import cache; ownership of the Minecraft texture is retained by Minecraft. */
final class BorrowedTextureViewCache {
    private GpuTextureView source;
    private TextureView view;

    TextureView get(RenderDevice device, GpuTextureView nextSource) {
        if (source != nextSource) {
            source = nextSource;
            view = GlTextureBridge.importColor(device, nextSource);
        }
        return view;
    }
}
