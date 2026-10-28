package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.FreecamFeature;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Candidate: FeatureManager key-free lookup requires callee proof and interface merge. */
@Mixin(LevelExtractor.class)
public abstract class WorldRendererEntityStateMixin {
    @Shadow
    protected abstract EntityRenderState extractEntity(Entity entity, float tickProgress);

    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void onFillEntityRenderStatesReturn(Camera camera, Frustum frustum, DeltaTracker ticks,
            LevelRenderState state, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.level != null
                && !Echo.eventsDisabled && Echo.featureManager != null) {
            FreecamFeature freecam = (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam.isEnabled()) {
                LocalPlayer player = MinecraftAccess.CLIENT.player;
                TickRateManager tickManager = MinecraftAccess.CLIENT.level.tickRateManager();
                float tickProgress = ticks.getGameTimeDeltaPartialTick(!tickManager.isEntityFrozen(player));
                EntityRenderState playerState = extractEntity(player, tickProgress);
                state.entityRenderStates.add(playerState);
            }
        }
    }
}
