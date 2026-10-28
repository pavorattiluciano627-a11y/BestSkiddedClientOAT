package dev.echo.render;

import dev.echo.client.render.NativeMemoryWrites;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.BlendMode;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.TextureView;
import dev.echo.render.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Preserves submission order by drawing consecutive runs of identical texture
 * references. Mapped instance storage grows independently of the texture runs.
 * This candidate's key/guard/cache applicability is documented in EFFECT-BATCH.md.
 */
public class IndexedTextureEffectBatch implements EffectBatch {
    private static final String IMAGE_SHADER = "shaders/wgsl/image/textured_quad.wgsl";
    private static final String SCREEN_SHADER = "shaders/wgsl/image/screen_sample_rounded.wgsl";

    private final int bytesPerInstance;
    private final RenderDevice device;
    private final Buffer cameraBuffer;
    private final boolean screenSampling;
    private final boolean premultiplied;
    private final GraphicsPipeline pipeline;
    private Buffer instanceBuffer;
    private final Sampler sampler;
    private final List<TextureView> textures = new ArrayList<>();
    private final IntList textureRunStarts = new IntArrayList();
    private TextureView previousTexture;
    private int capacity = 256;
    private int submittedCount;
    private int firstUnfinishedRun;
    private int pendingCount;

    /** Booleans retain the original order: screen sampling, depth, premultiplication. */
    public IndexedTextureEffectBatch(RenderDevice device, Buffer cameraBuffer,
            boolean screenSampling, boolean depthEnabled, boolean premultiplied) {
        this.device = device;
        this.cameraBuffer = cameraBuffer;
        this.screenSampling = screenSampling;
        this.premultiplied = premultiplied;
        bytesPerInstance = screenSampling ? 36 : 52;

        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, screenSampling ? SCREEN_SHADER : IMAGE_SHADER)
                .vertexAttribute(0, VertexFormat.Float4)
                .vertexAttribute(1, VertexFormat.Float)
                .vertexAttribute(2, VertexFormat.Float)
                .vertexAttribute(3, VertexFormat.Float)
                .vertexAttribute(4, VertexFormat.Int)
                .vertexAttribute(5, VertexFormat.Int);
        if (!screenSampling) {
            info.vertexAttribute(6, VertexFormat.Float4);
        }
        info.blendEnabled = true;
        info.blendMode = BlendMode.PREMULTIPLIED_ALPHA;
        info.depthTestEnabled = depthEnabled;
        info.depthWriteEnabled = depthEnabled;
        if (screenSampling) {
            info.pushConstantBytes = 12;
        }

        instanceBuffer = device.createBuffer(BufferInfo.builder().usage(1)
                .size((long) bytesPerInstance * capacity).dynamic(true).mapped(true).build());
        pipeline = device.createGraphicsPipeline(info);
        sampler = device.createSampler(SamplerInfo.builder()
                .filter(screenSampling ? Filter.LINEAR : Filter.NEAREST).build());
    }

    @Override
    public void draw(TextureView texture,
            float x, float y, float width, float height, float radius, float opacity,
            int tint, boolean mask, float u0, float v0, float u1, float v1, float depth) {
        if (submittedCount + pendingCount >= capacity) {
            growInstanceBuffer();
        }
        if (texture != previousTexture) {
            previousTexture = texture;
            textures.add(texture);
            textureRunStarts.add(submittedCount + pendingCount);
        }

        long address = instanceBuffer.mappedAddress() + (long) (submittedCount + pendingCount) * bytesPerInstance;
        NativeMemoryWrites.putFloat4(address, x, y, width, height);
        NativeMemoryWrites.putFloat(address + 16, depth);
        NativeMemoryWrites.putFloat(address + 20, radius);
        NativeMemoryWrites.putFloat(address + 24, opacity);
        NativeMemoryWrites.putInt(address + 28, tint);
        NativeMemoryWrites.putInt(address + 32, (mask ? 1 : 0) | (premultiplied ? 2 : 0));
        if (!screenSampling) {
            NativeMemoryWrites.putFloat4(address + 36, u0, v0, u1 - u0, v1 - v0);
        }
        pendingCount++;
    }

    private void growInstanceBuffer() {
        capacity *= 2;
        instanceBuffer = device.reallocateMappedBuffer(instanceBuffer,
                (long) bytesPerInstance * capacity,
                (long) (submittedCount + pendingCount) * bytesPerInstance);
    }

    @Override
    public void prepare(CommandEncoder encoder) {
        encoder.transitionBuffer(instanceBuffer.wholeSlice(), 1, BufferAccess.VERTEX_READ);
    }

    @Override
    public void reset() {
        submittedCount = 0;
        firstUnfinishedRun = 0;
        pendingCount = 0;
        previousTexture = null;
        textures.clear();
        textureRunStarts.clear();
    }

    @Override
    public void draw(CommandEncoder encoder, RenderPass pass) {
        if (pendingCount == 0) {
            return;
        }
        int pendingStart = submittedCount;
        int pendingEnd = submittedCount + pendingCount;
        pass.setPipeline(pipeline);
        if (screenSampling) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer constants = stack.mallocFloat(3);
                constants.put(0, MinecraftClient.getInstance().getWindow().getFramebufferWidth());
                constants.put(1, MinecraftClient.getInstance().getWindow().getFramebufferHeight());
                constants.put(2, pass.isYFlipped() ? 1.0F : 0.0F);
                pass.setPushConstants(MemoryUtil.memAddress(constants), 12);
            }
        }
        pass.setBuffer(0, cameraBuffer.wholeSlice());
        pass.setSampler(2, sampler);

        int runCount = textures.size();
        int run = firstUnfinishedRun;
        while (run < runCount && textureRunStarts.getInt(run) < pendingEnd) {
            int runStart = textureRunStarts.getInt(run);
            int runEnd = run + 1 < runCount ? textureRunStarts.getInt(run + 1) : pendingEnd;
            int drawStart = Math.max(runStart, pendingStart);
            int drawEnd = Math.min(runEnd, pendingEnd);
            if (drawEnd > drawStart) {
                pass.setVertexBuffer(instanceBuffer.slice((long) drawStart * bytesPerInstance,
                        (long) (drawEnd - drawStart) * bytesPerInstance));
                pass.setTexture(1, textures.get(run));
                pass.draw(4, drawEnd - drawStart, 0);
            }
            if (run + 1 >= runCount || textureRunStarts.getInt(run + 1) > pendingEnd) {
                break;
            }
            run++;
        }
        firstUnfinishedRun = run;
        submittedCount = pendingEnd;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instanceBuffer);
        device.release(sampler);
    }
}
