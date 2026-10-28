package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.FreecamFeature;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: FeatureManager key-free lookup requires callee proof and interface merge. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererEntityStateMixin {
    @Shadow
    protected abstract EntityRenderState getAndUpdateRenderState(Entity entity, float tickProgress);

    @Inject(method = "fillEntityRenderStates", at = @At("RETURN"))
    private void onFillEntityRenderStatesReturn(Camera camera, Frustum frustum, RenderTickCounter ticks,
            WorldRenderState state, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && !Echo.eventsDisabled && Echo.featureManager != null) {
            FreecamFeature freecam = (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam.isEnabled()) {
                ClientPlayerEntity player = MinecraftAccess.CLIENT.player;
                TickManager tickManager = MinecraftAccess.CLIENT.world.getTickManager();
                float tickProgress = ticks.getTickProgress(!tickManager.shouldSkipTick(player));
                EntityRenderState playerState = getAndUpdateRenderState(player, tickProgress);
                state.entityRenderStates.add(playerState);
            }
        }
    }
}
