package dev.echo.render;

/**
 * Mutable glyph-cache storage: advance/box metrics, outline band references and
 * optional pairwise kerning. The font loader fills these arrays from actual fonts.
 * Guard removal remains conditional in the source receipt.
 */
public class GlyphCoverageGrid {
    public final int firstCodePoint;
    public final int glyphCount;
    public final boolean[] hasOutline;
    public final float[] advanceWidths;
    public final float[][] glyphOffsets;
    public final float[][] glyphSizes;
    public final int[][] coverageBandReferences;
    public final float[][] kerningAdvances;
    public float ascent;
    public float lineHeight;

    public GlyphCoverageGrid(int firstCodePoint, int glyphCount, boolean kerning) {
        this.firstCodePoint = firstCodePoint;
        this.glyphCount = glyphCount;
        hasOutline = new boolean[glyphCount];
        advanceWidths = new float[glyphCount];
        glyphOffsets = new float[2][glyphCount];
        glyphSizes = new float[2][glyphCount];
        coverageBandReferences = new int[2][glyphCount];
        kerningAdvances = kerning ? new float[glyphCount][glyphCount] : null;
    }

    public int indexOf(int codePoint) {
        int index = codePoint - firstCodePoint;
        return index >= 0 && index < glyphCount ? index : -1;
    }
}
