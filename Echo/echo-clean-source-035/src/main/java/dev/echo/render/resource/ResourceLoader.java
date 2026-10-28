package dev.echo.render.resource;

/** Renderer asset-loading boundary. Implementations supply the original bytes/text. */
public interface ResourceLoader {
    String loadText(String path);

    byte[] loadBytes(String path);
}
