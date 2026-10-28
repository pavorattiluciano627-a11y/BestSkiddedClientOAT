package dev.echo.client.features.impl.utility;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.SelectedSlotEvent;
import dev.echo.client.events.lifecycle.ScreenChangeEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.inventory.InventoryScreenSlots;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.ExplosionDamage;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;

/** Main-hand totem selection, inventory restoration and predicted-explosion ownership. */
public class TotemHandFeature extends Feature {
    private static final double CRYSTAL_RANGE = 12.0;
    private static final int ANCHOR_RANGE = 10;
    private static BooleanSetting onPop;
    private static BooleanSetting predictive;
    private static BooleanSetting crystals;
    private static BooleanSetting anchors;
    private static BooleanSetting killable;
    private static IntSetting minDamage;
    private static BooleanSetting swapBackOnSafe;
    private static BooleanSetting onInventoryOpen;
    private static BooleanSetting swapBackOnClose;
    private static IntSetting inventoryChance;
    private int slotBeforeInventory;
    private int inventoryTotemSlot;
    private int predictedTotemSlot;
    private long predictionTick;
    private boolean protectingMainHand;

    public TotemHandFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Totem Hand"), dev.echo.client.bootstrap.CatalogText.literal("Swaps a totem into your main hand on pop, on inventory open, or when explosion damage is predicted"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        slotBeforeInventory = -1;
        inventoryTotemSlot = -1;
        predictedTotemSlot = -1;
        predictionTick = -1L;
        crystals.setVisibilityPredicate(ignored -> predictive.getValue());
        anchors.setVisibilityPredicate(ignored -> predictive.getValue());
        killable.setVisibilityPredicate(ignored -> predictive.getValue());
        minDamage.setVisibilityPredicate(ignored -> predictive.getValue() && !killable.getValue());
        swapBackOnSafe.setVisibilityPredicate(ignored -> predictive.getValue());
        swapBackOnClose.setVisibilityPredicate(ignored -> onInventoryOpen.getValue());
        inventoryChance.setVisibilityPredicate(ignored -> onInventoryOpen.getValue());
        predictive.tooltip("Prediction");
        crystals.tooltip("Prediction");
        anchors.tooltip("Prediction");
        killable.tooltip("Prediction");
        minDamage.tooltip("Prediction");
        swapBackOnSafe.tooltip("Prediction");
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearInventoryRestore();
        clearPrediction();
    }

    private int findTotemSlot() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING);
    }

    @Subscribe
    private void onPacket(PacketReceiveEvent event) {
        if (isOutsideWorld() || !onPop.getValue()) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket status)) return;
        if (status.getStatus() != 35) return;
        Entity entity = status.getEntity(client.world);
        if (!(entity instanceof PlayerEntity) || entity != client.player) return;
        int slot = findTotemSlot();
        if (slot != -1) InventoryItems.selectHotbarSlot(slot);
    }

    @Subscribe
    private void onScreenChange(ScreenChangeEvent event) {
        if (isOutsideWorld()) {
            clearInventoryRestore();
            return;
        }
        if (!InventoryScreenSlots.isPlayerInventoryScreen(event.getScreen())) {
            restoreAfterInventory();
            return;
        }
        clearInventoryRestore();
        if (!onInventoryOpen.getValue()) return;
        if (Math.random() * 100.0 >= inventoryChance.getValue()) return;
        int slot = findTotemSlot();
        if (slot == -1) return;
        int current = client.player.getInventory().getSelectedSlot();
        if (swapBackOnClose.getValue() && slot != current) {
            slotBeforeInventory = current;
            inventoryTotemSlot = slot;
        }
        InventoryItems.selectHotbarSlot(slot);
    }

    private void restoreAfterInventory() {
        int previous = slotBeforeInventory;
        int totem = inventoryTotemSlot;
        clearInventoryRestore();
        if (!swapBackOnClose.getValue()) return;
        if (previous < 0 || previous > 8) return;
        if (client.player.getInventory().getSelectedSlot() != totem) return;
        InventoryItems.selectHotbarSlot(previous);
    }

    private void clearInventoryRestore() {
        slotBeforeInventory = -1;
        inventoryTotemSlot = -1;
    }

    private void updatePrediction() {
        if (client.player.age == predictionTick) return;
        predictionTick = client.player.age;
        if (!predictive.getValue() || client.player.isCreative() || client.player.isSpectator()
                || !hasDangerousExplosion()) {
            clearPrediction();
            return;
        }
        boolean holdingTotem = client.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        predictedTotemSlot = holdingTotem ? -1 : findTotemSlot();
        protectingMainHand = holdingTotem || predictedTotemSlot != -1;
    }

    @Subscribe
    private void onSelectedSlot(SelectedSlotEvent event) {
        if (isOutsideWorld() || !protectingMainHand) return;
        int slot = event.getSelectedSlot();
        if (slot < 0 || slot > 8) return;
        if (client.player.getInventory().getStack(slot).getItem() == Items.TOTEM_OF_UNDYING) return;
        event.cancel();
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (isOutsideWorld()) return;
        updatePrediction();
        if (predictedTotemSlot == -1 || event.slot != predictedTotemSlot) return;
        if (DeferredActionQueue.selectedSlot() == predictedTotemSlot) return;
        if (DeferredActionQueue.ensureSelected(this, predictedTotemSlot, -1, true, null)) {
            // The original increments the click count; this is not event cancellation.
            event.addClick();
        }
    }

    private boolean hasDangerousExplosion() {
        float threshold = killable.getValue()
                ? client.player.getHealth() + client.player.getAbsorptionAmount() : minDamage.getValue();
        if (crystals.getValue()) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) continue;
                if (client.player.squaredDistanceTo(entity) > CRYSTAL_RANGE * CRYSTAL_RANGE) continue;
                if (ExplosionDamage.crystalDamage(client.player, entity.getEntityPos()) >= threshold) return true;
            }
        }
        if (anchors.getValue()) {
            BlockPos center = client.player.getBlockPos();
            for (BlockPos pos : BlockPos.iterate(center.add(-ANCHOR_RANGE, -ANCHOR_RANGE, -ANCHOR_RANGE),
                    center.add(ANCHOR_RANGE, ANCHOR_RANGE, ANCHOR_RANGE))) {
                // Two world reads preserve the original block-test/property-read order.
                if (!client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) continue;
                if (client.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0) continue;
                if (client.world.getEnvironmentAttributes().getAttributeValue(
                        EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, pos)) continue;
                if (ExplosionDamage.anchorDamage(client.player, pos, null) >= threshold) return true;
            }
        }
        return false;
    }

    public boolean isProtectingMainHand() {
        return isEnabled() && protectingMainHand;
    }

    public boolean isExplosionThreatPresent() {
        if (!isEnabled() || isOutsideWorld() || !predictive.getValue()) return false;
        if (client.player.isCreative() || client.player.isSpectator()) return false;
        return hasDangerousExplosion();
    }

    private void clearPrediction() {
        predictedTotemSlot = -1;
        protectingMainHand = false;
        if (DeferredActionQueue.contains(this)) {
            DeferredActionQueue.finish(this, swapBackOnSafe.getValue());
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(TotemHandFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.onPop);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.predictive);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.crystals);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.anchors);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.killable);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.minDamage);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.swapBackOnSafe);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.onInventoryOpen);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.swapBackOnClose);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, TotemHandFeature.inventoryChance);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(TotemHandFeature feature, dev.echo.client.setting.BooleanSetting preparedSwapBackOnClose, dev.echo.client.setting.BooleanSetting preparedAnchors, dev.echo.client.setting.IntSetting preparedInventoryChance, dev.echo.client.setting.BooleanSetting preparedPredictive, dev.echo.client.setting.BooleanSetting preparedKillable, dev.echo.client.setting.IntSetting preparedMinDamage, dev.echo.client.setting.BooleanSetting preparedCrystals, dev.echo.client.setting.BooleanSetting preparedOnPop, dev.echo.client.setting.BooleanSetting preparedSwapBackOnSafe, dev.echo.client.setting.BooleanSetting preparedOnInventoryOpen) {
        TotemHandFeature.swapBackOnClose = preparedSwapBackOnClose;
        TotemHandFeature.anchors = preparedAnchors;
        TotemHandFeature.inventoryChance = preparedInventoryChance;
        TotemHandFeature.predictive = preparedPredictive;
        TotemHandFeature.killable = preparedKillable;
        TotemHandFeature.minDamage = preparedMinDamage;
        TotemHandFeature.crystals = preparedCrystals;
        TotemHandFeature.onPop = preparedOnPop;
        TotemHandFeature.swapBackOnSafe = preparedSwapBackOnSafe;
        TotemHandFeature.onInventoryOpen = preparedOnInventoryOpen;
    }
}
