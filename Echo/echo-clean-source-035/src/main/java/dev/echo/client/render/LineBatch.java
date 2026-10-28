package dev.echo.client.render;

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

/** Complete algorithm candidate; device interfaces and conditional targets require merge/proof. */
public final class LineBatch implements RenderBatch {
    private static final int STRIDE = 28;
    private static final int VERTEX_BUFFER_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private final RenderDevice device;
    private final Buffer worldUniforms;
    private final GraphicsPipeline pipeline;
    private Buffer instances;
    private long capacity = 256L;
    private int count;

    public LineBatch(RenderDevice device, Buffer worldUniforms) {
        this.device = device;
        this.worldUniforms = worldUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/3d/line.wgsl")
                .vertexAttribute(0, VertexFormat.Float3).vertexAttribute(1, VertexFormat.Float3)
                .vertexAttribute(2, VertexFormat.Int);
        info.topology = PrimitiveTopology.TRIANGLE_STRIP;
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_BUFFER_USAGE)
                .size(capacity * STRIDE).mapped(true).dynamic(true).build());
    }

    public void add(double x1, double y1, double z1, double x2, double y2, double z2,
                    int color, Vec3 camera) {
        if (count >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) count * STRIDE;
        NativeMemoryWrites.putFloat3(address, (float) (x1 - camera.x), (float) (y1 - camera.y), (float) (z1 - camera.z));
        NativeMemoryWrites.putFloat3(address + 12, (float) (x2 - camera.x), (float) (y2 - camera.y), (float) (z2 - camera.z));
        NativeMemoryWrites.putInt(address + 24, color);
        count++;
    }

    private void grow() {
        capacity <<= 1;
        instances = device.reallocateMappedBuffer(instances, capacity * STRIDE, (long) count * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder encoder) {
        encoder.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder encoder, RenderPass pass) {
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.wholeSlice());
        pass.setBuffer(0, worldUniforms.wholeSlice());
        pass.draw(4, count, 0);
        count = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
    }
}
