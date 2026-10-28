package dev.echo.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads the client's plainly named bundled assets. */
public final class ClientResources {
    private static final String RESOURCE_ROOT = "assets/echo/";

    private ClientResources() { }

    /** Returns a stream for a relative asset path, or null when missing. The caller closes it. */
    public static InputStream open(String relativePath) {
        return ClientResources.class.getClassLoader().getResourceAsStream(RESOURCE_ROOT + relativePath);
    }

    /** Reads and closes an asset stream; missing or unreadable resources return null. */
    public static byte[] readBytes(String relativePath) {
        try (InputStream input = open(relativePath)) {
            return input == null ? null : input.readAllBytes();
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /** Reads a UTF-8 asset, or returns null when the byte resource is unavailable. */
    public static String readText(String relativePath) {
        byte[] contents = readBytes(relativePath);
        return contents == null ? null : new String(contents, StandardCharsets.UTF_8);
    }
}
