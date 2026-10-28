package dev.echo.client.render;

import dev.echo.render.EffectBatch;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.BlendMode;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Filter;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import dev.echo.render.vertex.VertexFormat;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Sixteen-texture effect batches with retained per-frame instance storage.
 * Uses the actual shader/record layouts, with key/guard/cache conditions retained
 * outside application source. Interface naming is reconciled in the API receipt.
 */
public class MultiTextureEffectBatch implements EffectBatch {
    private static final int TEXTURE_LIMIT = 16;
    private static final int SCREEN_STRIDE = 40;
    private static final int IMAGE_STRIDE = 56;
    private static final int VERTEX_USAGE = 1;
    private static final int TRANSFER_SOURCE_USAGE = 4;
    private static final int SAMPLED_TRANSFER_DESTINATION_USAGE = 5;
    private static final int VERTEX_STAGE = 1;
    private static final int WHITE_PIXEL_BYTES = 4;
    private static final int SCREEN_PUSH_CONSTANT_BYTES = 12;
    private static final int MASK_OR_FLIP_Y = 1;
    private static final int PREMULTIPLIED_INPUT = 2;
    private static final String IMAGE_SHADER = "shaders/wgsl/image/array.wgsl";
    // Original composite/array.wgsl hashes to the parent's preserved ac70b82ecfde4e2b asset.
    private static final String SCREEN_SHADER = "shaders/wgsl/image/screen_array_sample_rounded.wgsl";

    private final int stride;
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final boolean screenCoordinates;
    private final boolean premultipliedInput;
    private final GraphicsPipeline pipeline;
    private Buffer instances;
    private final Sampler sampler;
    private final Texture whiteTexture;
    private final TextureView whiteView;
    private final List<TextureView> textures = new ArrayList<>();
    private final TextureView[] boundTextures = new TextureView[TEXTURE_LIMIT];
    private int capacity = 256;
    private int firstInstance;
    private int pendingCount;

    public MultiTextureEffectBatch(RenderDevice device, Buffer cameraUniforms,
                                    boolean screenCoordinates, boolean depthTest, boolean premultipliedInput) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        this.screenCoordinates = screenCoordinates;
        this.premultipliedInput = premultipliedInput;
        stride = screenCoordinates ? SCREEN_STRIDE : IMAGE_STRIDE;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, screenCoordinates ? SCREEN_SHADER : IMAGE_SHADER)
                .vertexAttribute(0, VertexFormat.Float4)
                .vertexAttribute(1, VertexFormat.Int)
                .vertexAttribute(2, VertexFormat.Float)
                .vertexAttribute(3, VertexFormat.Float)
                .vertexAttribute(4, VertexFormat.Float)
                .vertexAttribute(5, VertexFormat.Int)
                .vertexAttribute(6, VertexFormat.Int);
        if (!screenCoordinates) {
            info.vertexAttribute(7, VertexFormat.Float4);
        }
        info.blendEnabled = true;
        info.blendMode = BlendMode.PREMULTIPLIED_ALPHA;
        info.depthTestEnabled = depthTest;
        info.depthWriteEnabled = depthTest;
        if (screenCoordinates) {
            info.pushConstantBytes = SCREEN_PUSH_CONSTANT_BYTES;
        }

        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size((long) stride * capacity)
                .dynamic(true).mapped(true).build());
        pipeline = device.createGraphicsPipeline(info);
        sampler = device.createSampler(SamplerInfo.builder().filter(screenCoordinates ? Filter.LINEAR : Filter.NEAREST).build());
        whiteTexture = device.createTexture(TextureInfo.builder().width(1).height(1)
                .format(TextureFormat.BGRA8Unorm).usage(SAMPLED_TRANSFER_DESTINATION_USAGE).build());
        Buffer upload = device.createBuffer(BufferInfo.builder().usage(TRANSFER_SOURCE_USAGE)
                .size(WHITE_PIXEL_BYTES).mapped(true).build());
        NativeMemoryWrites.putInt(upload.mappedAddress(), -1);
        TransferPass transfer = device.beginTransfer();
        transfer.uploadTexture(whiteTexture.views[0], upload.wholeSlice());
        transfer.end();
        device.release(upload);
        whiteView = whiteTexture.views[0];
    }

    @Override
    public void draw(TextureView texture, float x, float y, float width, float height,
                    float radius, float opacity, int tint, boolean maskOrFlipY,
                    float u0, float v0, float u1, float v1, float depth) {
        if (pendingCount >= TEXTURE_LIMIT) {
            throw new IllegalStateException("Image array batch must be flushed");
        }
        if (firstInstance + pendingCount >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) (firstInstance + pendingCount) * stride;
        NativeMemoryWrites.putFloat4(address, x, y, width, height);
        NativeMemoryWrites.putInt(address + 16, pendingCount);
        NativeMemoryWrites.putFloat(address + 20, depth);
        NativeMemoryWrites.putFloat(address + 24, radius);
        NativeMemoryWrites.putFloat(address + 28, opacity);
        NativeMemoryWrites.putInt(address + 32, tint);
        NativeMemoryWrites.putInt(address + 36,
                (maskOrFlipY ? MASK_OR_FLIP_Y : 0) | (premultipliedInput ? PREMULTIPLIED_INPUT : 0));
        if (!screenCoordinates) {
            NativeMemoryWrites.putFloat4(address + 40, u0, v0, u1 - u0, v1 - v0);
        }
        textures.add(texture);
        pendingCount++;
    }

    @Override
    public boolean isFull() {
        return pendingCount >= TEXTURE_LIMIT;
    }

    private void grow() {
        capacity *= 2;
        instances = device.reallocateMappedBuffer(instances, (long) stride * capacity,
                (long) (firstInstance + pendingCount) * stride);
    }

    @Override
    public void reset() {
        firstInstance = 0;
        pendingCount = 0;
        textures.clear();
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        if (pendingCount == 0) {
            return;
        }
        Arrays.fill(boundTextures, whiteView);
        for (int index = 0; index < pendingCount; index++) {
            boundTextures[index] = textures.get(firstInstance + index);
        }
        pass.setPipeline(pipeline);
        if (screenCoordinates) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer constants = stack.mallocFloat(3);
                constants.put(0, Minecraft.getInstance().getWindow().getWidth());
                constants.put(1, Minecraft.getInstance().getWindow().getHeight());
                constants.put(2, pass.isYFlipped() ? 1.0f : 0.0f);
                pass.setPushConstants(MemoryUtil.memAddress(constants), SCREEN_PUSH_CONSTANT_BYTES);
            }
        }
        pass.setVertexBuffer(instances.slice((long) firstInstance * stride, (long) pendingCount * stride));
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setTextures(1, boundTextures);
        pass.setSampler(2, sampler);
        pass.draw(4, pendingCount, 0);
        firstInstance += pendingCount;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        device.release(sampler);
        device.release(whiteTexture);
    }
}
