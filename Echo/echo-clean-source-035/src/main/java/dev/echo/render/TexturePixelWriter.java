package dev.echo.render;

import java.nio.ByteBuffer;

/** Writes packed skin colors into RGBA texture-upload memory. */
public final class TexturePixelWriter {
    private TexturePixelWriter() {
    }

    /** Appends one ARGB integer as red, green, blue, and alpha bytes. */
    public static void putRgba(ByteBuffer destination, int argb) {
        destination.put((byte) ((argb >> 16) & 255));
        destination.put((byte) ((argb >> 8) & 255));
        destination.put((byte) (argb & 255));
        destination.put((byte) ((argb >> 24) & 255));
    }
}
