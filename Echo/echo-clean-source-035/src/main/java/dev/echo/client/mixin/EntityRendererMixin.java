package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.FreecamFeature;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderStateTail(T entity, S state, float tickProgress, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && !Echo.eventsDisabled && Echo.featureManager != null) {
            FreecamFeature freecam = (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam != null && freecam.isEnabled()) {
                ClientPlayerEntity player = MinecraftAccess.CLIENT.player;
                if (entity == player) {
                    state.displayName = player.getDisplayName();
                    state.nameLabelPos = player.getAttachments().getPointNullable(
                        EntityAttachmentType.NAME_TAG, 0, player.getLerpedYaw(tickProgress));
                }
            }
        }
    }
}
