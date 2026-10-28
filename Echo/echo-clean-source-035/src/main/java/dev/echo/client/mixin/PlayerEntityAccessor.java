package dev.echo.client.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the player's space check at an offset. */
@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Invoker("isSpaceAroundPlayerEmpty")
    boolean invokeIsSpaceAroundPlayerEmpty(double offsetX, double offsetY, double offsetZ);
}
