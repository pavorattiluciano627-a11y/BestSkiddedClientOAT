package dev.echo.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Minecraft-owned GPU part of the item atlas. Device-independent texture import
 * and request/slot maps belong to the outer cache, not to this resource owner.
 */
public final class MinecraftItemAtlasSurface {
    public static final int SIZE = 2048;

    private final PoseStack matrices;
    private final ProjectionMatrixBuffer projection;
    private GpuTexture color;
    private GpuTextureView colorView;
    private GpuTexture depth;
    private GpuTextureView depthView;

    public MinecraftItemAtlasSurface() {
        this(new PoseStack(), new ProjectionMatrixBuffer("echo items"));
    }

    MinecraftItemAtlasSurface(PoseStack matrices, ProjectionMatrixBuffer projection) {
        this.matrices = matrices;
        this.projection = projection;
    }

    /**
     * 26.2 validates usage when a render pass is created, not at allocation: a
     * colour attachment needs USAGE_RENDER_ATTACHMENT (8) *and* USAGE_COPY_DST (1),
     * depth the same. Usage 12 omitted COPY_DST and threw
     *   IllegalStateException: Color texture must have USAGE_COPY_DST
     * on the first item draw, which took out every item-rendering module.
     * Vanilla's RenderTarget allocates with 15.
     */
    private static final int COLOR_USAGE = 1 | 2 | 4 | 8;
    private static final int DEPTH_USAGE = 1 | 2 | 8;

    /** Allocates the original named color/depth surfaces in their original order. */
    public void allocate() {
        allocate(RenderSystem.getDevice());
    }

    void allocate(GpuDevice device) {
        color = device.createTexture("echo items atlas", COLOR_USAGE, GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
        colorView = device.createTextureView(color);
        depth = device.createTexture("echo items atlas depth", DEPTH_USAGE, GpuFormat.D32_FLOAT, SIZE, SIZE, 1, 1);
        depthView = device.createTextureView(depth);
        device.createCommandEncoder().clearColorAndDepthTextures(color,
                new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F), depth, 1.0);
    }

    public GpuTextureView colorView() {
        return colorView;
    }

    public boolean hasColorTexture() {
        return color != null;
    }

    /**
     * Draws a tile using the original output overrides and flush order. The
     * caller determines whether an existing animated tile needs clearing.
     */
    public void renderTile(TrackingItemStackRenderState state, int x, int y, int tileSize, boolean clearExisting) {
        int bottom = y + tileSize;
        if (clearExisting) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    color, new org.joml.Vector4f(0.0F, 0.0F, 0.0F, 0.0F), depth, 1.0,
                    x, SIZE - bottom, tileSize, tileSize);
        }
        matrices.pushPose();
        matrices.translate(x + tileSize / 2.0F, y + tileSize / 2.0F, 0.0F);
        matrices.scale(tileSize, -tileSize, tileSize);
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;
        RenderSystem.setProjectionMatrix(projection.getBuffer(
                new org.joml.Matrix4f().setOrtho(0.0F, SIZE, SIZE, 0.0F, 1000.0F, 21000.0F)),
                ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(x, SIZE - bottom, tileSize, tileSize);
        Minecraft client = Minecraft.getInstance();
        client.gameRenderer.lighting().setupFor(
                state.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        state.submit(matrices, client.gameRenderer.handAndScreenSubmitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0);
        client.gameRenderer.featureRenderDispatcher().renderAllFeatures(
                client.gameRenderer.handAndScreenSubmitNodeStorage);
        client.gameRenderer.renderBuffers().stagedVertexBuffer().endFrame();
        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        matrices.popPose();
    }

    /**
     * Releases only the four atlas resources. Projection ownership and outer
     * cache resets are deliberately handled by their own original operations.
     */
    public void releaseTextures() {
        if (color == null) {
            return;
        }
        colorView.close();
        color.close();
        depthView.close();
        depth.close();
        color = null;
        colorView = null;
        depth = null;
        depthView = null;
    }
}
