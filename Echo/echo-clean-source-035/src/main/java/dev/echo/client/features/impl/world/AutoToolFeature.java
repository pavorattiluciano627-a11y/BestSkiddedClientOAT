package dev.echo.client.features.impl.world;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;

/** Selects the fastest qualifying hotbar tool and restores the original slot on release. */
public class AutoToolFeature extends Feature {
    private static BooleanSetting switchBack;
    private static BooleanSetting requireClick;
    private static BooleanSetting requireShift;
    private int previousSlot;

    public AutoToolFeature() {
        super(new FeatureInfo(CatalogText.literal("Auto Tool"),
            CatalogText.literal("Automatically selects best tool"), Category.WORLD));
    }

    @Override
    public void postInitialize() {
        previousSlot = -1;
        super.postInitialize();
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.hitResult instanceof BlockHitResult hit
                && !client.level.getBlockState(hit.getBlockPos()).isAir()
                && (!requireClick.getValue() || client.options.keyAttack.isDown())
                && (!requireShift.getValue() || client.options.keyShift.isDown())) {
            int bestSlot = findBestToolSlot(hit.getBlockPos());
            if (bestSlot != -1) {
                if (previousSlot == -1) {
                    previousSlot = client.player.getInventory().getSelectedSlot();
                }
                client.player.getInventory().setSelectedSlot(bestSlot);
            } else if (switchBack.getValue() && previousSlot != -1) {
                client.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        } else if (switchBack.getValue() && previousSlot != -1) {
            client.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }

    private int findBestToolSlot(BlockPos position) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = miningSpeed(stack, position);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private float miningSpeed(ItemStack stack, BlockPos position) {
        if (stack.isEmpty()) {
            return 0.0f;
        }
        float speed = stack.getDestroySpeed(client.level.getBlockState(position));
        if (speed > 1.0f) {
            var efficiency = client.level.registryAccess().get(Enchantments.EFFICIENCY);
            if (efficiency.isPresent()) {
                int level = EnchantmentHelper.getItemEnchantmentLevel(efficiency.get(), stack);
                if (level > 0) {
                    speed += level * level + 1;
                }
            }
        }
        return speed;
    }

    public static void appendBootstrapSettings(AutoToolFeature feature) {
        CatalogSettings.append(feature, switchBack);
        CatalogSettings.append(feature, requireClick);
        CatalogSettings.append(feature, requireShift);
    }

    public static void assignBootstrapSettings(AutoToolFeature feature, BooleanSetting preparedRequireClick,
            BooleanSetting preparedSwitchBack, BooleanSetting preparedRequireShift) {
        requireClick = preparedRequireClick;
        switchBack = preparedSwitchBack;
        requireShift = preparedRequireShift;
    }
}
