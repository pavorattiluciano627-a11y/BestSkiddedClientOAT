package dev.echo.client.mixin;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Redirect(method = "raycast", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private static float redirectRaycastAtGetYaw(PlayerEntity player) {
        return player != MinecraftAccess.CLIENT.player ? player.getYaw()
            : RotationEvent.dispatch(RotationContext.LOOK, player.getYaw(), player.getPitch()).getYaw();
    }

    @Redirect(method = "raycast", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getPitch()F"))
    private static float redirectRaycastAtGetPitch(PlayerEntity player) {
        return player != MinecraftAccess.CLIENT.player ? player.getPitch()
            : RotationEvent.dispatch(RotationContext.LOOK, player.getYaw(), player.getPitch()).getPitch();
    }
}
