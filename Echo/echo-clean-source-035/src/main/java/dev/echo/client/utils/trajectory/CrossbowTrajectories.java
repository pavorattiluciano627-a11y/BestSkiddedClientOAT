package dev.echo.client.utils.trajectory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/** Crossbow projectile order and mode selection from the original factory. */
public final class CrossbowTrajectories {
    private CrossbowTrajectories() {}

    public static List<List<Vec3>> fromPlayer(Player player, ItemStack crossbow, float tickDelta) {
        if (player == null || Minecraft.getInstance().level == null || crossbow == null || crossbow.isEmpty()) {
            return Collections.emptyList();
        }
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        // Original LayerHandler.updatePath: null component means no charged stacks.
        List<ItemStack> projectiles = charged == null ? List.<ItemStack>of() : charged.itemCopies();
        int count = projectiles.isEmpty() ? 1 : projectiles.size();
        float spread = 0.0F;
        var multishot = Minecraft.getInstance().level.registryAccess().get(Enchantments.MULTISHOT);
        if (multishot.isPresent()) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(multishot.get(), crossbow);
            if (level > 0) {
                spread += (float) level * 10.0F;
            }
        }
        float angleStep = count == 1 ? 0.0F : 2.0F * spread / (float) (count - 1);
        float angleOffset = (float) ((count - 1) % 2) * angleStep / 2.0F;
        float sign = 1.0F;
        List<List<Vec3>> paths = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            // Integer division is essential, as is flipping sign after each shot.
            float angle = angleOffset + sign * (float) ((index + 1) / 2) * angleStep;
            sign = -sign;
            ItemStack projectile = projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.get(index);
            if (!projectile.isEmpty() && projectile.is(Items.FIREWORK_ROCKET)) {
                FireworkSimulation simulation = new FireworkSimulation(player, angle, projectile, index, tickDelta);
                paths.add(TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step));
            } else {
                CrossbowArrowSimulation simulation = new CrossbowArrowSimulation(player, angle, tickDelta);
                paths.add(TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step));
            }
        }
        return paths;
    }

    public static List<Vec3> fromFirework(FireworkRocketEntity rocket) {
        if (rocket == null || Minecraft.getInstance().level == null) {
            return Collections.emptyList();
        }
        FireworkSimulation simulation = new FireworkSimulation(rocket);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
