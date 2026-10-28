package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.client.particle.ParticleUv;
import dev.echo.render.TriangleBatch;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;

/** Ordered world batches drawn against Minecraft's borrowed scene depth attachment. */
public final class DepthTestedWorldGeometry extends WorldGeometry {
    private final RenderDevice device;
    private final List<RenderBatch> batches = new ArrayList<>();
    private final BoxBatch boxes;
    private final TracerBatch tracers;
    private final LineBatch lines;
    private final TriangleBatch triangles;
    private final ParticleBatch particles;
    private Object minecraftDepthSource;
    private TextureView minecraftDepthView;

    public DepthTestedWorldGeometry(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        boxes = new BoxBatch(device, cameraUniforms);
        tracers = new TracerBatch(device, cameraUniforms);
        lines = new LineBatch(device, cameraUniforms);
        triangles = new TriangleBatch(device, cameraUniforms);
        particles = new ParticleBatch(device, cameraUniforms);
        batches.add(boxes);
        batches.add(tracers);
        batches.add(lines);
        batches.add(triangles);
        batches.add(particles);
    }

    @Override
    public void beginFrame() {
        batches.forEach(RenderBatch::reset);
    }

    public void renderTo(TextureView colorTarget) {
        CommandEncoder commands = Echo.commandEncoder;
        batches.forEach(batch -> batch.prepare(commands));
        TextureView depth = depthTexture();
        commands.transitionTexture(colorTarget, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        commands.transitionTexture(depth, 8, TextureAccess.DEPTH_ATTACHMENT, TextureLayout.DEPTH_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(colorTarget, OptionalInt.of(0), depth, OptionalDouble.empty());
        device.setViewportToTexture(pass, colorTarget);
        batches.forEach(batch -> batch.draw(commands, pass));
        pass.end();
    }

    private TextureView depthTexture() {
        GpuTextureView source = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance()).getDepthAttachmentView();
        if (source != minecraftDepthSource) {
            minecraftDepthSource = source;
            minecraftDepthView = GlTextureBridge.importView(device, source, device.depthFormat());
        }
        return minecraftDepthView;
    }

    @Override
    public void destroy() {
        batches.forEach(RenderBatch::destroy);
    }

    @Override
    public void drawBox(double x, double y, double z, double width, double height, double depth, int color) {
        boxes.add(x, y, z, width, height, depth, color);
    }

    @Override
    public void drawTracer(double x, double y, double z, int color) {
        tracers.add(x, y, z, color, Echo.worldRenderer.cameraPosition);
    }

    @Override
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        lines.add(x1, y1, z1, x2, y2, z2, color, Echo.worldRenderer.cameraPosition);
    }

    @Override
    public void drawTriangle(double x1, double y1, double z1, int color1,
                             double x2, double y2, double z2, int color2,
                             double x3, double y3, double z3, int color3) {
        triangles.addTriangle(Echo.worldRenderer.cameraPosition,
                x1, y1, z1, color1, x2, y2, z2, color2, x3, y3, z3, color3);
    }

    @Override
    public void drawParticle(double x, double y, double z, float size, int color, ParticleUv uv, float rotation) {
        particles.add(x, y, z, size, color, uv, rotation, Echo.worldRenderer.cameraPosition);
    }
}
