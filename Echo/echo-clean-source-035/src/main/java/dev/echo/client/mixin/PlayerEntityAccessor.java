package dev.echo.client.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the player's space check at an offset. */
@Mixin(Player.class)
public interface PlayerEntityAccessor {
    @Invoker("canFallAtLeast")
    boolean invokeIsSpaceAroundPlayerEmpty(double offsetX, double offsetY, double offsetZ);
}
