package dev.echo.client.render;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Minecraft-owned GPU part of the item atlas. Device-independent texture import
 * and request/slot maps belong to the outer cache, not to this resource owner.
 */
public final class MinecraftItemAtlasSurface {
    public static final int SIZE = 2048;

    private final MatrixStack matrices;
    private final ProjectionMatrix2 projection;
    private GpuTexture color;
    private GpuTextureView colorView;
    private GpuTexture depth;
    private GpuTextureView depthView;

    public MinecraftItemAtlasSurface() {
        this(new MatrixStack(), new ProjectionMatrix2("echo items", -1000.0F, 1000.0F, true));
    }

    MinecraftItemAtlasSurface(MatrixStack matrices, ProjectionMatrix2 projection) {
        this.matrices = matrices;
        this.projection = projection;
    }

    /** Allocates the original named color/depth surfaces in their original order. */
    public void allocate() {
        allocate(RenderSystem.getDevice());
    }

    void allocate(GpuDevice device) {
        color = device.createTexture("echo items atlas", 12, TextureFormat.RGBA8, SIZE, SIZE, 1, 1);
        colorView = device.createTextureView(color);
        depth = device.createTexture("echo items atlas depth", 8, TextureFormat.DEPTH32, SIZE, SIZE, 1, 1);
        depthView = device.createTextureView(depth);
        device.createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
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
    public void renderTile(KeyedItemRenderState state, int x, int y, int tileSize, boolean clearExisting) {
        int bottom = y + tileSize;
        if (clearExisting) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    color, 0, depth, 1.0, x, SIZE - bottom, tileSize, tileSize);
        }
        matrices.push();
        matrices.translate(x + tileSize / 2.0F, y + tileSize / 2.0F, 0.0F);
        matrices.scale(tileSize, -tileSize, tileSize);
        RenderSystem.outputColorTextureOverride = colorView;
        RenderSystem.outputDepthTextureOverride = depthView;
        RenderSystem.setProjectionMatrix(projection.set(SIZE, SIZE), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.enableScissorForRenderTypeDraws(x, SIZE - bottom, tileSize, tileSize);
        MinecraftClient client = MinecraftClient.getInstance();
        client.gameRenderer.getDiffuseLighting().setShaderLights(
                state.isSideLit() ? DiffuseLighting.Type.ITEMS_3D : DiffuseLighting.Type.ITEMS_FLAT);
        state.render(matrices, client.gameRenderer.getEntityRenderCommandQueue(), 15728880, OverlayTexture.DEFAULT_UV, 0);
        client.gameRenderer.getEntityRenderDispatcher().render();
        client.getBufferBuilders().getEntityVertexConsumers().draw();
        RenderSystem.disableScissorForRenderTypeDraws();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        matrices.pop();
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
