package dev.echo.render;

import dev.echo.client.render.NativeMemoryWrites;
import dev.echo.client.render.RenderBatch;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.pipeline.PrimitiveTopology;
import dev.echo.render.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;

/**
 * Instanced world triangles with camera-relative positions and per-vertex color.
 * Candidate only: callers must pass the original world-render camera snapshot.
 */
public class TriangleBatch implements RenderBatch {
    private static final int BYTES_PER_INSTANCE = 48;
    private final RenderDevice device;
    private final Buffer cameraBuffer;
    private final GraphicsPipeline pipeline;
    private Buffer instanceBuffer;
    private long capacity = 256L;
    private int instanceCount;

    public TriangleBatch(RenderDevice device, Buffer cameraBuffer) {
        this.device = device;
        this.cameraBuffer = cameraBuffer;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/3d/triangle.wgsl")
                .vertexAttribute(0, VertexFormat.Float3)
                .vertexAttribute(1, VertexFormat.Float3)
                .vertexAttribute(2, VertexFormat.Float3)
                .vertexAttribute(3, VertexFormat.Int)
                .vertexAttribute(4, VertexFormat.Int)
                .vertexAttribute(5, VertexFormat.Int);
        info.topology = PrimitiveTopology.TRIANGLE_LIST;
        info.cullEnabled = false;
        info.depthWriteEnabled = false;
        pipeline = device.createGraphicsPipeline(info);
        instanceBuffer = device.createBuffer(BufferInfo.builder().usage(1)
                .size(capacity * BYTES_PER_INSTANCE).mapped(true).dynamic(true).build());
    }

    public void addTriangle(Vec3 camera,
            double firstX, double firstY, double firstZ, int firstColor,
            double secondX, double secondY, double secondZ, int secondColor,
            double thirdX, double thirdY, double thirdZ, int thirdColor) {
        if (instanceCount >= capacity) {
            growInstanceBuffer();
        }
        long address = instanceBuffer.mappedAddress() + (long) instanceCount * BYTES_PER_INSTANCE;
        NativeMemoryWrites.putFloat3(address,
                (float) (firstX - camera.x), (float) (firstY - camera.y), (float) (firstZ - camera.z));
        NativeMemoryWrites.putFloat3(address + 12,
                (float) (secondX - camera.x), (float) (secondY - camera.y), (float) (secondZ - camera.z));
        NativeMemoryWrites.putFloat3(address + 24,
                (float) (thirdX - camera.x), (float) (thirdY - camera.y), (float) (thirdZ - camera.z));
        NativeMemoryWrites.putInt3(address + 36, firstColor, secondColor, thirdColor);
        instanceCount++;
    }

    private void growInstanceBuffer() {
        capacity <<= 1;
        instanceBuffer = device.reallocateMappedBuffer(instanceBuffer,
                capacity * BYTES_PER_INSTANCE, (long) instanceCount * BYTES_PER_INSTANCE);
    }

    @Override
    public void prepare(CommandEncoder encoder) {
        encoder.transitionBuffer(instanceBuffer.wholeSlice(), 1, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder encoder, RenderPass pass) {
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instanceBuffer.wholeSlice());
        pass.setBuffer(0, cameraBuffer.wholeSlice());
        pass.draw(3, instanceCount, 0);
        instanceCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instanceBuffer);
    }
}
