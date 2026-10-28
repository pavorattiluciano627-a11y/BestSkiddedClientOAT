package dev.echo.render;

import dev.echo.client.render.NativeMemoryWrites;
import dev.echo.client.render.RenderBatch;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.BlendMode;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.vertex.VertexFormat;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

/**
 * Rounded rectangles, shadows and color-picker gradients in one instanced batch.
 * Candidate only: source/target and mutable-guard obligations remain in the receipt.
 */
public class QuadBatch implements RenderBatch {
    private static final int BYTES_PER_INSTANCE = 72;
    private static final int RECTANGLE = 0;
    private static final int SHADOW = 1;
    private static final int HUE = 2;
    private static final int SATURATION_BRIGHTNESS = 3;
    private static final int ALPHA = 4;

    private final RenderDevice device;
    private final Buffer cameraBuffer;
    private final GraphicsPipeline pipeline;
    private Buffer instanceBuffer;
    private int capacity = 512;
    private int submittedCount;
    private int pendingCount;

    public QuadBatch(RenderDevice device, Buffer cameraBuffer) {
        this.device = device;
        this.cameraBuffer = cameraBuffer;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/quad.wgsl")
                .vertexAttribute(0, VertexFormat.Float4)
                .vertexAttribute(1, VertexFormat.Float4)
                .vertexAttribute(2, VertexFormat.Int4)
                .vertexAttribute(3, VertexFormat.Float4)
                .vertexAttribute(4, VertexFormat.Int2);
        info.blendMode = BlendMode.PREMULTIPLIED_ALPHA;
        pipeline = device.createGraphicsPipeline(info);
        instanceBuffer = device.createBuffer(BufferInfo.builder().usage(1)
                .size((long) BYTES_PER_INSTANCE * capacity).dynamic(true).mapped(true).build());
    }

    public void addRectangle(float x, float y, float width, float height,
            float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius,
            int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor,
            float borderWidth, int borderColor, float depth) {
        writeInstance(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius,
                topLeftColor, topRightColor, bottomLeftColor, bottomRightColor,
                depth, borderWidth, 1.0F, RECTANGLE, borderColor);
    }

    public void addShadow(float x, float y, float width, float height,
            float radius, float sigma, int color, float depth) {
        writeInstance(x, y, width, height, radius, 0, 0, 0,
                color, color, color, color, depth, sigma, 1.0F, SHADOW, 0);
    }

    public void addHueGradient(float x, float y, float width, float height, float opacity, float depth) {
        addHueGradient(x, y, width, height, 0.0F, opacity, depth);
    }

    public void addHueGradient(float x, float y, float width, float height, float radius, float opacity, float depth) {
        writeInstance(x, y, width, height, radius, radius, radius, radius,
                0, 0, 0, 0, depth, 0.0F, opacity, HUE, 0);
    }

    public void addSaturationBrightnessGradient(float x, float y, float width, float height,
            float hue, float opacity, float depth) {
        addSaturationBrightnessGradient(x, y, width, height, 0.0F, hue, opacity, depth);
    }

    public void addSaturationBrightnessGradient(float x, float y, float width, float height,
            float radius, float hue, float opacity, float depth) {
        writeInstance(x, y, width, height, radius, radius, radius, radius,
                0, 0, 0, 0, depth, hue, opacity, SATURATION_BRIGHTNESS, 0);
    }

    public void addAlphaGradient(float x, float y, float width, float height,
            float red, float green, float blue, float opacity, float depth) {
        addAlphaGradient(x, y, width, height, 0.0F, red, green, blue, opacity, depth);
    }

    public void addAlphaGradient(float x, float y, float width, float height,
            float radius, float red, float green, float blue, float opacity, float depth) {
        int color = ARGB.colorFromFloat(1.0F, red, green, blue);
        writeInstance(x, y, width, height, radius, radius, radius, radius,
                color, color, color, color, depth, 0.0F, opacity, ALPHA, 0);
    }

    private void writeInstance(float x, float y, float width, float height,
            float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius,
            int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor,
            float depth, float effectParameter, float opacity, int kind, int borderColor) {
        if (submittedCount + pendingCount >= capacity) {
            growInstanceBuffer();
        }
        long address = instanceBuffer.mappedAddress() + (long) (submittedCount + pendingCount) * BYTES_PER_INSTANCE;
        NativeMemoryWrites.putFloat4(address, x, y, width, height);
        NativeMemoryWrites.putFloat4(address + 16, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius);
        MemoryUtil.memPutInt(address + 32, topLeftColor);
        MemoryUtil.memPutInt(address + 36, topRightColor);
        MemoryUtil.memPutInt(address + 40, bottomLeftColor);
        MemoryUtil.memPutInt(address + 44, bottomRightColor);
        NativeMemoryWrites.putFloat4(address + 48, depth, effectParameter, opacity, 0.0F);
        NativeMemoryWrites.putInt2(address + 64, kind, borderColor);
        pendingCount++;
    }

    private void growInstanceBuffer() {
        capacity *= 2;
        instanceBuffer = device.reallocateMappedBuffer(instanceBuffer, (long) BYTES_PER_INSTANCE * capacity,
                (submittedCount + pendingCount) * (long) BYTES_PER_INSTANCE);
    }

    @Override
    public void prepare(CommandEncoder encoder) {
        encoder.transitionBuffer(instanceBuffer.wholeSlice(), 1, BufferAccess.VERTEX_READ);
    }

    @Override
    public void reset() {
        submittedCount = 0;
        pendingCount = 0;
    }

    @Override
    public void draw(CommandEncoder encoder, RenderPass pass) {
        if (pendingCount == 0) {
            return;
        }
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instanceBuffer.slice((long) submittedCount * BYTES_PER_INSTANCE,
                (long) pendingCount * BYTES_PER_INSTANCE));
        pass.setBuffer(0, cameraBuffer.wholeSlice());
        pass.draw(4, pendingCount, 0);
        submittedCount += pendingCount;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instanceBuffer);
    }
}
