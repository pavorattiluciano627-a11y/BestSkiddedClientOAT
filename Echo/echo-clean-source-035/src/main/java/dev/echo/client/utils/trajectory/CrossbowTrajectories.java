package dev.echo.client.utils.trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/** Crossbow projectile order and mode selection from the original factory. */
public final class CrossbowTrajectories {
    private CrossbowTrajectories() {}

    public static List<List<Vec3d>> fromPlayer(PlayerEntity player, ItemStack crossbow, float tickDelta) {
        if (player == null || MinecraftClient.getInstance().world == null || crossbow == null || crossbow.isEmpty()) {
            return Collections.emptyList();
        }
        ChargedProjectilesComponent charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
        // Original LayerHandler.updatePath: null component means no charged stacks.
        List<ItemStack> projectiles = charged == null ? List.of() : charged.getProjectiles();
        int count = projectiles.isEmpty() ? 1 : projectiles.size();
        float spread = 0.0F;
        var multishot = MinecraftClient.getInstance().world.getRegistryManager().getOptionalEntry(Enchantments.MULTISHOT);
        if (multishot.isPresent()) {
            int level = EnchantmentHelper.getLevel(multishot.get(), crossbow);
            if (level > 0) {
                spread += (float) level * 10.0F;
            }
        }
        float angleStep = count == 1 ? 0.0F : 2.0F * spread / (float) (count - 1);
        float angleOffset = (float) ((count - 1) % 2) * angleStep / 2.0F;
        float sign = 1.0F;
        List<List<Vec3d>> paths = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            // Integer division is essential, as is flipping sign after each shot.
            float angle = angleOffset + sign * (float) ((index + 1) / 2) * angleStep;
            sign = -sign;
            ItemStack projectile = projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.get(index);
            if (!projectile.isEmpty() && projectile.isOf(Items.FIREWORK_ROCKET)) {
                FireworkSimulation simulation = new FireworkSimulation(player, angle, projectile, index, tickDelta);
                paths.add(TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step));
            } else {
                CrossbowArrowSimulation simulation = new CrossbowArrowSimulation(player, angle, tickDelta);
                paths.add(TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step));
            }
        }
        return paths;
    }

    public static List<Vec3d> fromFirework(FireworkRocketEntity rocket) {
        if (rocket == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        FireworkSimulation simulation = new FireworkSimulation(rocket);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
