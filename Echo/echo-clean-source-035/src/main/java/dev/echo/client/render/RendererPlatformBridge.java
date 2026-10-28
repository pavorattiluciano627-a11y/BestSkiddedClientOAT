package dev.echo.client.render;

import dev.echo.client.resources.ClientResources;
import dev.echo.render.resource.ResourceLoader;

/** Candidate shared interface implementation; renderer-core-05 owns ResourceLoader. */
public final class RendererPlatformBridge implements ResourceLoader {
    @Override
    public String loadText(String path) {
        return ClientResources.readText(path);
    }

    @Override
    public byte[] loadBytes(String path) {
        return ClientResources.readBytes(path);
    }
}
