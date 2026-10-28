package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.features.impl.render.FreecamFeature;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"), cancellable = true)
    private void onUpdateReturn(Level world, Entity focusedEntity, boolean thirdPerson,
                                boolean inverseView, float tickProgress, CallbackInfo callback) {
        if (!Echo.eventsDisabled && world != null && focusedEntity != null
                && MinecraftAccess.CLIENT.player != null && Echo.featureManager != null) {
            FreecamFeature freecam = (FreecamFeature) Echo.featureManager.findFeature(FreecamFeature.class);
            if (freecam != null && freecam.isEnabled()) {
                Vec3 position = freecam.getCameraPosition();
                if (position == null) {
                    return;
                }
                setRotation(freecam.getCameraYaw(), freecam.getCameraPitch());
                setPosition(position);
            }
        }
    }
}
