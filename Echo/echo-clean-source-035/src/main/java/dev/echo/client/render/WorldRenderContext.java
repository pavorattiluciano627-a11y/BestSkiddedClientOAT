package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.Echo;
import dev.echo.render.EffectBatch;
import dev.echo.render.IndexedTextureEffectBatch;
import dev.echo.client.screens.EchoScreen;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureAccess;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureLayout;
import dev.echo.render.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * World-frame camera data, world-geometry targets and final color composition.
 * This is a conditional source candidate; its original linkage/control-state
 * obligations and shared provider signatures are listed in WORLD-CONTEXT.md.
 */
public final class WorldRenderContext {
    private static final int UNIFORM_USAGE = 2;
    private static final long CAMERA_UNIFORM_BYTES = 160;
    private static final long COMPOSITION_UNIFORM_BYTES = 64;
    private static final int COLOR_TEXTURE_USAGE = 9;

    /** Snapshot used by world batches; it is updated when setMatrices is called. */
    public Vec3d cameraPosition = Vec3d.ZERO;
    /** Projection multiplied by view, retained independently of both input matrices. */
    public Matrix4f viewProjectionMatrix;

    private final DepthTestedWorldGeometry depthTestedGeometry;
    private final OverlayWorldGeometry overlayGeometry;
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final Buffer compositionUniforms;
    private final EffectBatch worldComposite;
    private final EffectBatch backgroundComposite;
    private Texture worldColorTexture;
    private GpuTextureView minecraftColorSource;
    private TextureView minecraftColorView;

    public WorldRenderContext(RenderDevice device) {
        this.device = device;
        cameraUniforms = device.createBuffer(BufferInfo.builder().usage(UNIFORM_USAGE)
                .size(CAMERA_UNIFORM_BYTES).mapped(true).dynamic(true).build());
        depthTestedGeometry = new DepthTestedWorldGeometry(device, cameraUniforms);
        overlayGeometry = new OverlayWorldGeometry(device, cameraUniforms);
        compositionUniforms = device.createBuffer(BufferInfo.builder().usage(UNIFORM_USAGE)
                .size(COMPOSITION_UNIFORM_BYTES).dynamic(true).mapped(true).build());
        worldComposite = device.capabilities().sampledImageArrays()
                ? new MultiTextureEffectBatch(device, compositionUniforms, false, false, true)
                : new IndexedTextureEffectBatch(device, compositionUniforms, false, false, true);
        // Keep the second capability read and construction after the first batch.
        backgroundComposite = device.capabilities().sampledImageArrays()
                ? new MultiTextureEffectBatch(device, compositionUniforms, true, false, false)
                : new IndexedTextureEffectBatch(device, compositionUniforms, true, false, false);
        allocateWorldColor();
    }

    private void allocateWorldColor() {
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        worldColorTexture = device.createTexture(TextureInfo.builder()
                .width(framebuffer.textureWidth).height(framebuffer.textureHeight)
                .format(TextureFormat.BGRA8Unorm).usage(COLOR_TEXTURE_USAGE).build());
        worldColorTexture.views[0].yFlipped = GlTextureBridge.isYFlipped();
    }

    /** Writes the original 160-byte camera block in its shader-visible layout. */
    public void setMatrices(Matrix4f view, Matrix4f projection) {
        Camera camera = MinecraftRenderState.getCamera(MinecraftClient.getInstance());
        cameraPosition = camera.getCameraPos();
        viewProjectionMatrix = new Matrix4f(projection).mul(view);
        long address = cameraUniforms.mappedAddress();
        NativeMemoryWrites.putMatrix4f(address, view);
        NativeMemoryWrites.putMatrix4f(address + 64L, projection);
        NativeMemoryWrites.putFloat3(address + 128L,
                (float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        NativeMemoryWrites.putFloat2(address + 144L, framebuffer.textureWidth, framebuffer.textureHeight);
    }

    /** Resizes the offscreen target and resets batches before world-render subscribers run. */
    public void beginFrame() {
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        if (worldColorTexture.width != framebuffer.textureWidth
                || worldColorTexture.height != framebuffer.textureHeight) {
            device.release(worldColorTexture);
            allocateWorldColor();
        }
        // Obtain mapped storage before evaluating the orientation/projection,
        // retaining the original receiver/argument evaluation order.
        long address = compositionUniforms.mappedAddress();
        Matrix4f projection = GlTextureBridge.isYFlipped()
                ? new Matrix4f().ortho(0.0F, GuiDimensions.width(), 0.0F, GuiDimensions.height(),
                        -1000.0F, 1000.0F, device.isVulkan())
                : GuiProjection.create(device);
        NativeMemoryWrites.putMatrix4f(address, projection);
        depthTestedGeometry.beginFrame();
        overlayGeometry.beginFrame();
        worldComposite.reset();
        backgroundComposite.reset();
    }

    /** Draws both world layers, prepares the backdrop, then queues or emits composition. */
    public void flush() {
        depthTestedGeometry.renderTo(worldColorTexture.views[0]);
        overlayGeometry.renderTo(worldColorTexture.views[0]);
        // The original fetches the global encoder after both geometry passes.
        CommandEncoder commands = Echo.commandEncoder;
        if (!(MinecraftRenderState.getScreen() instanceof EchoScreen)) {
            Echo.uiRenderer.updateBackdrop(commands);
        }
        commands.transitionTexture(worldColorTexture.views[0], 2,
                TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        worldComposite.draw(worldColorTexture.views[0], 0.0F, 0.0F,
                GuiDimensions.width(), GuiDimensions.height(), 0.0F, 1.0F, 0.0F);
        if (Echo.externalRendererActive) {
            return;
        }
        beginTarget(commands, mainColorTexture(), OptionalInt.empty());
    }

    /** Emits the queued world composite to a target without clearing its depth. */
    public void beginTarget(CommandEncoder commands, TextureView target, OptionalInt clearColor) {
        worldComposite.prepare(commands);
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, clearColor, null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        worldComposite.draw(commands, pass);
        pass.end();
    }

    /** Composes a supplied background followed by world overlays into an external target. */
    public void compositeToTarget(CommandEncoder commands, TextureView target, TextureView source) {
        commands.transitionTexture(source, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        backgroundComposite.draw(source, 0.0F, 0.0F,
                GuiDimensions.width(), GuiDimensions.height(), 0.0F, 1.0F,
                0, GlTextureBridge.isYFlipped(), 0.0F);
        backgroundComposite.prepare(commands);
        worldComposite.prepare(commands);
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, OptionalInt.of(0xFF000000), null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        backgroundComposite.draw(commands, pass);
        worldComposite.draw(commands, pass);
        pass.end();
    }

    /** Imports the current Minecraft-owned color view once per source identity. */
    public TextureView mainColorTexture() {
        GpuTextureView source = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance()).getColorAttachmentView();
        if (source != minecraftColorSource) {
            minecraftColorSource = source;
            minecraftColorView = GlTextureBridge.importColor(device, source);
        }
        return minecraftColorView;
    }

    /** Original teardown sequence; borrowed Minecraft attachments are never released. */
    public void destroy() {
        worldComposite.destroy();
        backgroundComposite.destroy();
        depthTestedGeometry.destroy();
        overlayGeometry.destroy();
    }

    public DepthTestedWorldGeometry getDepthTestedGeometry() {
        return depthTestedGeometry;
    }

    public OverlayWorldGeometry getOverlayGeometry() {
        return overlayGeometry;
    }
}
