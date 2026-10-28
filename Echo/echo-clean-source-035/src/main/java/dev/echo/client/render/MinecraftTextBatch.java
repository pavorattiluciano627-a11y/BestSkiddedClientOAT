package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.TextureView;
import dev.echo.render.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.BakedGlyphImpl;
import net.minecraft.client.font.GlyphProvider;
import net.minecraft.text.StyleSpriteSource;

/** Minecraft glyph metrics and atlas-run batching, retaining actual named Yarn glyph types. */
public class MinecraftTextBatch implements RenderBatch {
    private static final int STRIDE = 44;
    private static final int VERTEX_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private static final String SHADER = "shaders/wgsl/mc_text.wgsl";

    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final Sampler sampler;
    private Buffer instances;
    private int capacity = 1024;
    private int firstInstance;
    private int pendingCount;
    private int firstTextureRun;
    private final List<TextureView> textureRuns = new ArrayList<>();
    private final IntList runStarts = new IntArrayList();
    private TextureView currentTexture;
    private final Map<GpuTextureView, TextureView> importedTextures = new HashMap<>();

    public MinecraftTextBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER)
                .vertexAttribute(0, VertexFormat.Float4).vertexAttribute(1, VertexFormat.Float4)
                .vertexAttribute(2, VertexFormat.Int2).vertexAttribute(3, VertexFormat.Float);
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size((long) STRIDE * capacity)
                .dynamic(true).mapped(true).build());
        sampler = device.createSampler(SamplerInfo.builder().filter(Filter.NEAREST).build());
    }

    public BakedGlyph[] bake(CharSequence text) {
        GlyphProvider provider = MinecraftClient.getInstance().textRenderer.fonts.getGlyphs(StyleSpriteSource.DEFAULT);
        BakedGlyph[] glyphs = new BakedGlyph[text.length()];
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character != '\n') glyphs[index] = provider.get(character);
        }
        return glyphs;
    }

    public float draw(BakedGlyph[] glyphs, float x, float y, float size, int color, float depth) {
        float scale = size / 8.0f;
        float lineStart = x;
        for (BakedGlyph glyph : glyphs) {
            if (glyph == null) {
                x = lineStart;
                y += fontHeight(size);
                continue;
            }
            if (glyph instanceof BakedGlyphImpl drawable) {
                addGlyph(drawable, x, y, scale, color, depth);
            }
            x += glyph.getMetrics().getAdvance() * scale;
        }
        return x;
    }

    public float measure(BakedGlyph[] glyphs, float size) {
        float scale = size / 8.0f;
        float lineWidth = 0.0f;
        float maximum = 0.0f;
        for (BakedGlyph glyph : glyphs) {
            if (glyph == null) {
                maximum = Math.max(maximum, lineWidth);
                lineWidth = 0.0f;
            } else {
                lineWidth += glyph.getMetrics().getAdvance() * scale;
            }
        }
        return Math.max(maximum, lineWidth);
    }

    public float fontHeight(float size) {
        // Present in the original classfile; the older typed source view omitted this null check.
        Objects.requireNonNull(MinecraftClient.getInstance().textRenderer);
        return size * 9.0f / 8.0f;
    }

    private void addGlyph(BakedGlyphImpl glyph, float x, float y, float scale, int color, float depth) {
        if (firstInstance + pendingCount >= capacity) grow();
        TextureView texture = importedTextures.computeIfAbsent(glyph.textureView,
                source -> GlTextureBridge.importColor(device, source));
        if (texture != currentTexture) {
            currentTexture = texture;
            textureRuns.add(texture);
            runStarts.add(firstInstance + pendingCount);
        }
        int monochrome = glyph.textureView.texture().getFormat() == TextureFormat.RED8 ? 1 : 0;
        long address = instances.mappedAddress() + (long) (firstInstance + pendingCount) * STRIDE;
        NativeMemoryWrites.putFloat4(address, x + glyph.minX * scale, y + glyph.minY * scale,
                (glyph.maxX - glyph.minX) * scale, (glyph.maxY - glyph.minY) * scale);
        NativeMemoryWrites.putFloat4(address + 16, glyph.minU, glyph.minV, glyph.maxU, glyph.maxV);
        NativeMemoryWrites.putInt2(address + 32, color, monochrome);
        NativeMemoryWrites.putFloat(address + 40, depth);
        pendingCount++;
    }

    private void grow() {
        capacity *= 2;
        instances = device.reallocateMappedBuffer(instances, (long) STRIDE * capacity,
                (long) (firstInstance + pendingCount) * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void reset() {
        firstInstance = 0;
        pendingCount = 0;
        firstTextureRun = 0;
        currentTexture = null;
        textureRuns.clear();
        runStarts.clear();
        importedTextures.clear();
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        if (pendingCount == 0) return;
        int start = firstInstance;
        int end = firstInstance + pendingCount;
        pass.setPipeline(pipeline);
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setSampler(2, sampler);
        int totalRuns = textureRuns.size();
        int run = firstTextureRun;
        for (; run < totalRuns; run++) {
            if (runStarts.getInt(run) >= end) break;
            int runStart = runStarts.getInt(run);
            int runEnd = run + 1 < totalRuns ? runStarts.getInt(run + 1) : end;
            int from = Math.max(runStart, start);
            int to = Math.min(runEnd, end);
            if (to > from) {
                pass.setVertexBuffer(instances.slice((long) from * STRIDE, (long) (to - from) * STRIDE));
                pass.setTexture(1, textureRuns.get(run));
                pass.draw(4, to - from, 0);
            }
            if (run + 1 >= totalRuns || runStarts.getInt(run + 1) > end) break;
        }
        firstTextureRun = run;
        firstInstance = end;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        device.release(sampler);
    }
}
