package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.FreecamFeature;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderStateTail(T entity, S state, float tickProgress, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.level != null
                && !Echo.eventsDisabled && Echo.featureManager != null) {
            FreecamFeature freecam = (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam != null && freecam.isEnabled()) {
                LocalPlayer player = MinecraftAccess.CLIENT.player;
                if (entity == player) {
                    state.nameTag = player.getDisplayName();
                    state.nameTagAttachment = player.getAttachments().getNullable(
                        EntityAttachment.NAME_TAG, 0, player.getYRot(tickProgress));
                }
            }
        }
    }
}
