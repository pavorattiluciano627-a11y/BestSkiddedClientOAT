package dev.echo.client.render;

import dev.echo.client.Echo;
import dev.echo.client.particle.ParticleUv;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.BlendMode;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.sampler.Sampler;
import dev.echo.render.sampler.SamplerInfo;
import dev.echo.render.texture.TextureView;
import dev.echo.render.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;

/** Additive, camera-facing particle instances; conditional source/cache domains are in the receipt. */
public class ParticleBatch implements RenderBatch {
    private static final int STRIDE = 40;
    private static final int VERTEX_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private static final String SHADER = "shaders/wgsl/3d/particle.wgsl";

    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final Sampler sampler;
    private Buffer instances;
    private long capacity = 256;
    private int count;

    public ParticleBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER)
                .vertexAttribute(0, VertexFormat.Float3)
                .vertexAttribute(1, VertexFormat.Float)
                .vertexAttribute(2, VertexFormat.Int)
                .vertexAttribute(3, VertexFormat.Float4)
                .vertexAttribute(4, VertexFormat.Float);
        info.blendMode = BlendMode.ADDITIVE;
        info.depthWriteEnabled = false;
        info.cullEnabled = true;
        pipeline = device.createGraphicsPipeline(info);
        sampler = device.createSampler(SamplerInfo.builder().build());
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size(capacity * STRIDE)
                .mapped(true).dynamic(true).build());
    }

    public void add(double x, double y, double z, float size, int color,
                    ParticleUv uv, float rotation, Vec3 camera) {
        if (count >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) count * STRIDE;
        NativeMemoryWrites.putFloat3(address,
                (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z));
        NativeMemoryWrites.putFloat(address + 12, size);
        NativeMemoryWrites.putInt(address + 16, color);
        NativeMemoryWrites.putFloat4(address + 20, uv.minU(), uv.minV(), uv.maxU(), uv.maxV());
        NativeMemoryWrites.putFloat(address + 36, rotation);
        count++;
    }

    private void grow() {
        capacity <<= 1;
        instances = device.reallocateMappedBuffer(instances, capacity * STRIDE, (long) count * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        // The original obtains the atlas before checking count, including empty batches.
        TextureView atlas = Echo.particleManager.getTexture();
        if (count == 0 || atlas == null) {
            count = 0;
            return;
        }
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.wholeSlice());
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setTexture(1, atlas);
        pass.setSampler(2, sampler);
        pass.draw(4, count, 0);
        count = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        device.release(sampler);
    }
}
