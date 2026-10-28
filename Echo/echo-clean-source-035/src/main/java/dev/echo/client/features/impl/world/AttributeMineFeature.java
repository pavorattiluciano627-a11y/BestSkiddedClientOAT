package dev.echo.client.features.impl.world;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
                ((ClientPlayerInteractionManagerAccessor) client.gameMode).invokeSyncSelectedSlot();
                pendingToolSlot = -1;
            }
            return;
        }
        if (!client.options.keyAttack.isDown()) {
            lastTarget = null;
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            lastTarget = null;
            return;
        }
        BlockPos position = hit.getBlockPos();
        BlockState state = client.level.getBlockState(position);
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
        Inventory inventory = client.player.getInventory();
        inventory.setSelectedSlot(efficiencySlot);
        ((ClientPlayerInteractionManagerAccessor) client.gameMode).invokeSyncSelectedSlot();
        if (fastestSlot != -1 && fastestSlot != efficiencySlot) {
            pendingToolSlot = fastestSlot;
            switchDelay = 2;
        }
    }

    private int findHighestEfficiencySlot() {
        Inventory inventory = client.player.getInventory();
        int bestSlot = -1;
        int bestLevel = 0;
        var efficiency = client.level.registryAccess().get(Enchantments.EFFICIENCY);
        if (efficiency.isEmpty()) return -1;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            int level = EnchantmentHelper.getItemEnchantmentLevel(efficiency.get(), stack);
            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private int findFastestToolSlot(BlockState state) {
        Inventory inventory = client.player.getInventory();
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }
}
