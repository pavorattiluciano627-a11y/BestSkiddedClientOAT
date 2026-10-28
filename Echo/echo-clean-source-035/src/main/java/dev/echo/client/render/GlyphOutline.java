package dev.echo.client.render;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import org.lwjgl.stb.STBTruetype;

/**
 * Structured candidate for STB contours encoded as normalized quadratic segments.
 * Member-target and loop-invariant proofs are still required; see needs.json.
 */
public final class GlyphOutline {
    public final List<float[]> curves = new ArrayList<>();
    private final float scale;
    private final float left;
    private final float top;
    private final float width;
    private final float height;
    private final float padding;
    private float currentX;
    private float currentY;
    private float contourX;
    private float contourY;
    private boolean contourOpen;

    public GlyphOutline(float scale, float left, float top, float width, float height, float padding) {
        this.scale = scale;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.padding = padding;
    }

    public boolean readCodepoint(STBTTFontinfo font, int codepoint) {
        STBTTVertex.Buffer vertices = STBTruetype.stbtt_GetCodepointShape(font, codepoint);
        if (vertices == null || !vertices.hasRemaining()) {
            return false;
        }
        for (int index = 0; index < vertices.limit(); index++) {
            STBTTVertex vertex = vertices.get(index);
            switch (vertex.type()) {
                case STBTruetype.STBTT_vmove -> moveTo(vertex.x(), vertex.y());
                case STBTruetype.STBTT_vline -> lineTo(vertex.x(), vertex.y());
                case STBTruetype.STBTT_vcurve -> quadraticTo(vertex.cx(), vertex.cy(), vertex.x(), vertex.y());
                case STBTruetype.STBTT_vcubic -> cubicTo(vertex.cx(), vertex.cy(), vertex.cx1(), vertex.cy1(),
                        vertex.x(), vertex.y());
                default -> { /* Unknown STB vertex kinds fall through in the original switch. */ }
            }
        }
        closeContour();
        STBTruetype.stbtt_FreeShape(font, vertices);
        return !curves.isEmpty();
    }

    private void moveTo(float x, float y) {
        closeContour();
        currentX = x;
        currentY = y;
        contourX = x;
        contourY = y;
        contourOpen = true;
    }

    private void lineTo(float x, float y) {
        appendQuadratic(currentX, currentY, (currentX + x) * 0.5f, (currentY + y) * 0.5f, x, y);
        currentX = x;
        currentY = y;
    }

    private void quadraticTo(float controlX, float controlY, float x, float y) {
        appendQuadratic(currentX, currentY, controlX, controlY, x, y);
        currentX = x;
        currentY = y;
    }

    /** Preserves the original one-quadratic approximation, without adaptive subdivision. */
    private void cubicTo(float control1X, float control1Y, float control2X, float control2Y,
                         float x, float y) {
        float controlX = (3.0f * (control1X + control2X) - (currentX + x)) * 0.25f;
        float controlY = (3.0f * (control1Y + control2Y) - (currentY + y)) * 0.25f;
        appendQuadratic(currentX, currentY, controlX, controlY, x, y);
        currentX = x;
        currentY = y;
    }

    private void closeContour() {
        if (contourOpen && (currentX != contourX || currentY != contourY)) {
            lineTo(contourX, contourY);
        }
        contourOpen = false;
    }

    private void appendQuadratic(float x0, float y0, float x1, float y1, float x2, float y2) {
        curves.add(new float[]{normalizeX(x0), normalizeY(y0), normalizeX(x1), normalizeY(y1),
                normalizeX(x2), normalizeY(y2)});
    }

    private float normalizeX(float x) {
        return ((x - left) * scale + padding) / width;
    }

    private float normalizeY(float y) {
        return ((top - y) * scale + padding) / height;
    }
}
