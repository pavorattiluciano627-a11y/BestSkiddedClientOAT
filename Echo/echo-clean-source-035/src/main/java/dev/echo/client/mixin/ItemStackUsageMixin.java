package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.interaction.ItemUsageTickEvent;
import dev.echo.client.utils.targeting.AttackQueries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackUsageMixin {
    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void postKineticWeaponUsage(Level world, LivingEntity user, int remainingUseTicks,
            CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager != null && world.isClientSide() && user == Minecraft.getInstance().player) {
            ItemStack stack = (ItemStack) (Object) this;
            if (stack.has(DataComponents.KINETIC_WEAPON)) {
                new ItemUsageTickEvent(user, stack, remainingUseTicks,
                        AttackQueries.findKineticWeaponTargets(user, stack, remainingUseTicks)).post();
            }
        }
    }
}
