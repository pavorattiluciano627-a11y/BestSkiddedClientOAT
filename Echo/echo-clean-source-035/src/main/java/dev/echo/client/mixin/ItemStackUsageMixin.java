package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.interaction.ItemUsageTickEvent;
import dev.echo.client.utils.targeting.AttackQueries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackUsageMixin {
    @Inject(method = "usageTick", at = @At("HEAD"))
    private void postKineticWeaponUsage(World world, LivingEntity user, int remainingUseTicks,
            CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (Echo.featureManager != null && world.isClient() && user == MinecraftClient.getInstance().player) {
            ItemStack stack = (ItemStack) (Object) this;
            if (stack.contains(DataComponentTypes.KINETIC_WEAPON)) {
                new ItemUsageTickEvent(user, stack, remainingUseTicks,
                        AttackQueries.findKineticWeaponTargets(user, stack, remainingUseTicks)).post();
            }
        }
    }
}
