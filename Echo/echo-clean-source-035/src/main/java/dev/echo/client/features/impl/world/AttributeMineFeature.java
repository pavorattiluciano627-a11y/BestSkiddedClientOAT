package dev.echo.client.features.impl.world;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

/** Two-stage tool selection. Conditional original linkage domain: analysis/receipt.json. */
public class AttributeMineFeature extends Feature {
    private BlockPos lastTarget;
    private int pendingToolSlot;
    private int switchDelay;

    public AttributeMineFeature() {
        super(new FeatureInfo(CatalogText.literal("Attribute Mine"),
                CatalogText.literal("Technique to mine faster"), Category.WORLD));
    }

    @Override
    public void postInitialize() {
        lastTarget = null;
        pendingToolSlot = -1;
        switchDelay = 0;
        super.postInitialize();
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (pendingToolSlot != -1) {
            --switchDelay;
            if (switchDelay <= 0) {
                client.player.getInventory().setSelectedSlot(pendingToolSlot);
                ((ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
                pendingToolSlot = -1;
            }
            return;
        }
        if (!client.options.attackKey.isPressed()) {
            lastTarget = null;
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            lastTarget = null;
            return;
        }
        BlockPos position = hit.getBlockPos();
        BlockState state = client.world.getBlockState(position);
        if (state.isAir()) {
            lastTarget = null;
            return;
        }
        if (position.equals(lastTarget)) return;
        lastTarget = position;
        int efficiencySlot = findHighestEfficiencySlot();
        // The original scans both even when no Efficiency enchantment is available.
        int fastestSlot = findFastestToolSlot(state);
        if (efficiencySlot == -1) return;
        PlayerInventory inventory = client.player.getInventory();
        inventory.setSelectedSlot(efficiencySlot);
        ((ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
        if (fastestSlot != -1 && fastestSlot != efficiencySlot) {
            pendingToolSlot = fastestSlot;
            switchDelay = 2;
        }
    }

    private int findHighestEfficiencySlot() {
        PlayerInventory inventory = client.player.getInventory();
        int bestSlot = -1;
        int bestLevel = 0;
        var efficiency = client.world.getRegistryManager().getOptionalEntry(Enchantments.EFFICIENCY);
        if (efficiency.isEmpty()) return -1;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            int level = EnchantmentHelper.getLevel(efficiency.get(), stack);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private int findFastestToolSlot(BlockState state) {
        PlayerInventory inventory = client.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
