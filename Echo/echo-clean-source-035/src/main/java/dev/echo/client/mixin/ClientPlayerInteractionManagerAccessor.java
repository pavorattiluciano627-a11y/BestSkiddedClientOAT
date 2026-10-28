package dev.echo.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the interaction manager's slot synchronization and piercing attack. */
@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void invokeSyncSelectedSlot();

    @Invoker("attackWithPiercingWeapon")
    void invokeAttackWithPiercingWeapon(PiercingWeaponComponent weapon);
}
