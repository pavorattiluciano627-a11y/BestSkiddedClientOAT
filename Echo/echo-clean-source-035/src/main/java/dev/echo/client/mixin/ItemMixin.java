package dev.echo.client.mixin;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Redirect(method = "getPlayerPOVHitResult", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"), require = 0)
    private static float redirectRaycastAtGetYaw(Player player) {
        return player != MinecraftAccess.CLIENT.player ? player.getYRot()
            : RotationEvent.dispatch(RotationContext.LOOK, player.getYRot(), player.getXRot()).getYaw();
    }

    @Redirect(method = "getPlayerPOVHitResult", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"), require = 0)
    private static float redirectRaycastAtGetPitch(Player player) {
        return player != MinecraftAccess.CLIENT.player ? player.getXRot()
            : RotationEvent.dispatch(RotationContext.LOOK, player.getYRot(), player.getXRot()).getPitch();
    }
}
