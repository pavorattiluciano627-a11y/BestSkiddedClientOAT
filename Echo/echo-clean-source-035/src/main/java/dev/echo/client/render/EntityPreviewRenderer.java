package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.mixin.SpecialGuiElementRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Candidate preview implementation; accessor contract and opaque-state elimination are pending. */
public final class EntityPreviewRenderer {
    private static GuiEntityRenderer renderer;

    private EntityPreviewRenderer() {}

    public static EntityRenderState prepare(LivingEntity entity) {
        var entityRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = entityRenderer.createRenderState(entity, 1.0f);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.lightCoords = 15728880;
        state.nameTag = null;
        if (state instanceof AvatarRenderState player) {
            player.scoreText = null;
        }
        return state;
    }

    public static GpuTextureView render(EntityRenderState state, int width, int height, float scale,
                                        Vector3f translation, Quaternionf rotation) {
        GuiEntityRenderer renderer = renderer();
        var element = new net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState(state, translation, rotation, null,
                0, 0, width, height, scale, null);
        renderer.prepare(element, new GuiRenderState(),
                Minecraft.getInstance().gameRenderer.featureRenderDispatcher(), 1);
        return ((SpecialGuiElementRendererAccessor) renderer).getTextureView();
    }

    private static GuiEntityRenderer renderer() {
        if (renderer == null) {
            Minecraft client = Minecraft.getInstance();
            renderer = new GuiEntityRenderer(client.getEntityRenderDispatcher());
        }
        return renderer;
    }
}
