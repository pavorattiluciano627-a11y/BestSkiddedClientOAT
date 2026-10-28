package dev.echo.client.render;

/** Row sizing for the original 2048-wide glyph-data textures. */
public final class GlyphAtlasRows {
    public static final int WIDTH = 2048;

    private GlyphAtlasRows() {}

    public static int rowsFor(int texelCount) {
        return Math.max(1, (texelCount + WIDTH - 1) / WIDTH);
    }
}
