package dev.echo.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the interaction manager's slot synchronization and piercing attack. */
@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void invokeSyncSelectedSlot();

    @Invoker("piercingAttack")
    void invokeAttackWithPiercingWeapon(PiercingWeapon weapon);
}
