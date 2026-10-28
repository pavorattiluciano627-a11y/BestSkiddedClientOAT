package dev.echo.client.render;

import dev.echo.render.GlyphCoverageGrid;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.vertex.VertexFormat;

/** Quadratic-outline text with native instance storage and per-font kerning/line metrics. */
public class OutlineTextBatch implements RenderBatch {
    private static final int STRIDE = 32;
    private static final int VERTEX_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private static final int FRAGMENT_STAGE = 2;
    private static final String SHADER = "shaders/wgsl/slug_text.wgsl";
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final GlyphAtlas atlas;
    private Buffer instances;
    private int capacity = 1024;
    private int firstInstance;
    private int pendingCount;

    public OutlineTextBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        atlas = new GlyphAtlas(device);
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER)
                .vertexAttribute(0, VertexFormat.Float2).vertexAttribute(1, VertexFormat.Float2)
                .vertexAttribute(2, VertexFormat.Int2).vertexAttribute(3, VertexFormat.Int)
                .vertexAttribute(4, VertexFormat.Float);
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size((long) STRIDE * capacity)
                .dynamic(true).mapped(true).build());
    }

    public float draw(int fontOrdinal, CharSequence text, float x, float y, float size, int color, float depth) {
        GlyphCoverageGrid font = atlas.font(fontOrdinal);
        float lineStart = x;
        float baseline = y + font.ascent * size;
        int previous = -1;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                x = lineStart;
                baseline += font.lineHeight * size;
                previous = -1;
                continue;
            }
            int glyph = font.indexOf(character);
            if (glyph < 0) continue;
            if (font.kerningAdvances != null && previous >= 0) {
                x += font.kerningAdvances[previous][glyph] * size;
            }
            previous = glyph;
            if (!font.hasOutline[glyph]) {
                x += font.advanceWidths[glyph] * size;
                continue;
            }
            if (firstInstance + pendingCount >= capacity) grow();
            long address = instances.mappedAddress() + (long) (firstInstance + pendingCount) * STRIDE;
            NativeMemoryWrites.putFloat2(address,
                    x + font.glyphOffsets[0][glyph] * size, baseline - font.glyphOffsets[1][glyph] * size);
            NativeMemoryWrites.putFloat2(address + 8, font.glyphSizes[0][glyph] * size, font.glyphSizes[1][glyph] * size);
            NativeMemoryWrites.putInt2(address + 16, font.coverageBandReferences[0][glyph], font.coverageBandReferences[1][glyph]);
            NativeMemoryWrites.putInt(address + 24, color);
            NativeMemoryWrites.putFloat(address + 28, depth);
            pendingCount++;
            x += font.advanceWidths[glyph] * size;
        }
        return x;
    }

    public float fontHeight(int fontOrdinal, float size) {
        return atlas.font(fontOrdinal).lineHeight * size;
    }

    public float measureText(int fontOrdinal, CharSequence text, float size) {
        GlyphCoverageGrid font = atlas.font(fontOrdinal);
        float width = 0.0f;
        float maximum = 0.0f;
        int previous = -1;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                maximum = Math.max(maximum, width);
                width = 0.0f;
                previous = -1;
                continue;
            }
            int glyph = font.indexOf(character);
            if (glyph < 0) continue;
            if (font.kerningAdvances != null && previous >= 0) {
                width += font.kerningAdvances[previous][glyph] * size;
            }
            previous = glyph;
            width += font.advanceWidths[glyph] * size;
        }
        return Math.max(maximum, width);
    }

    public float advanceX(int fontOrdinal, CharSequence text, float x, float size) {
        GlyphCoverageGrid font = atlas.font(fontOrdinal);
        float lineStart = x;
        int previous = -1;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\n') {
                x = lineStart;
                previous = -1;
                continue;
            }
            int glyph = font.indexOf(character);
            if (glyph < 0) continue;
            if (font.kerningAdvances != null && previous >= 0) {
                x += font.kerningAdvances[previous][glyph] * size;
            }
            previous = glyph;
            x += font.advanceWidths[glyph] * size;
        }
        return x;
    }

    private void grow() {
        capacity *= 2;
        instances = device.reallocateMappedBuffer(instances, (long) STRIDE * capacity,
                (long) (firstInstance + pendingCount) * STRIDE);
    }

    @Override public void reset() { firstInstance = 0; pendingCount = 0; }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionTexture(atlas.curvesTexture.views[0], FRAGMENT_STAGE, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        commands.transitionTexture(atlas.bandTableTexture.views[0], FRAGMENT_STAGE, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        commands.transitionTexture(atlas.bandIndicesTexture.views[0], FRAGMENT_STAGE, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        if (pendingCount == 0) return;
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.slice((long) firstInstance * STRIDE, (long) pendingCount * STRIDE));
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setTexture(1, atlas.curvesTexture.views[0]);
        pass.setTexture(2, atlas.bandTableTexture.views[0]);
        pass.setTexture(3, atlas.bandIndicesTexture.views[0]);
        pass.draw(4, pendingCount, 0);
        firstInstance += pendingCount;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        atlas.destroy(device);
    }
}
