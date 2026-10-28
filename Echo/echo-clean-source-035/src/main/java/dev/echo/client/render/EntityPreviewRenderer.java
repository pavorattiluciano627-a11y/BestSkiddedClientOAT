package dev.echo.client.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.echo.client.mixin.SpecialGuiElementRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.EntityGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.special.EntityGuiElementRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Candidate preview implementation; accessor contract and opaque-state elimination are pending. */
public final class EntityPreviewRenderer {
    private static EntityGuiElementRenderer renderer;

    private EntityPreviewRenderer() {}

    public static EntityRenderState prepare(LivingEntity entity) {
        var entityRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state = entityRenderer.getAndUpdateRenderState(entity, 1.0f);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.light = 15728880;
        state.displayName = null;
        if (state instanceof PlayerEntityRenderState player) {
            player.playerName = null;
        }
        return state;
    }

    public static GpuTextureView render(EntityRenderState state, int width, int height, float scale,
                                        Vector3f translation, Quaternionf rotation) {
        EntityGuiElementRenderer renderer = renderer();
        var element = new EntityGuiElementRenderState(state, translation, rotation, null,
                0, 0, width, height, scale, null);
        renderer.render(element, new GuiRenderState(), 1);
        return ((SpecialGuiElementRendererAccessor) renderer).getTextureView();
    }

    private static EntityGuiElementRenderer renderer() {
        if (renderer == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            renderer = new EntityGuiElementRenderer(client.getBufferBuilders().getEntityVertexConsumers(),
                    client.getEntityRenderDispatcher());
        }
        return renderer;
    }
}
