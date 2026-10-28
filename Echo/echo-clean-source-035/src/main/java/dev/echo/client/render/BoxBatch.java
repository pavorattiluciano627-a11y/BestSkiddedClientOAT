package dev.echo.client.render;

import dev.echo.client.Echo;
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
import net.minecraft.util.math.Vec3d;

/** Original instanced box mesh: one position/extent/color instance draws 36 vertices. */
public final class BoxBatch implements RenderBatch {
    private static final int INSTANCE_BYTES = 28;
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private Buffer instances;
    private long capacity = 256L;
    private int instanceCount;

    public BoxBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/3d/box.wgsl")
                .vertexAttribute(0, VertexFormat.Float3)
                .vertexAttribute(1, VertexFormat.Float3)
                .vertexAttribute(2, VertexFormat.Int);
        info.blendEnabled = false;
        info.topology = PrimitiveTopology.TRIANGLE_LIST;
        info.cullEnabled = true;
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(1).size(capacity * INSTANCE_BYTES)
                .mapped(true).dynamic(true).build());
    }

    /** The last three doubles are extents, not the maximum corner. */
    public void add(double x, double y, double z, double width, double height, double depth, int color) {
        if (instanceCount >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) instanceCount * INSTANCE_BYTES;
        Vec3d camera = Echo.worldRenderer.cameraPosition;
        NativeMemoryWrites.putFloat3(address, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z));
        NativeMemoryWrites.putFloat3(address + 12L, (float) width, (float) height, (float) depth);
        NativeMemoryWrites.putInt(address + 24L, color);
        instanceCount++;
    }

    private void grow() {
        capacity <<= 1;
        instances = device.reallocateMappedBuffer(instances, capacity * INSTANCE_BYTES,
                (long) instanceCount * INSTANCE_BYTES);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), 1, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.wholeSlice());
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.draw(36, instanceCount, 0);
        instanceCount = 0;
    }

    @Override
    public void destroy() {
        // Original ActivityRegistry.validateTable() is exactly RETURN (one byte).
        // Do not invent resource deletion here: disposal policy requires owner review.
    }
}
