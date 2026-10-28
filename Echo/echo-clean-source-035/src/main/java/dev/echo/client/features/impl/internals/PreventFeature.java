package dev.echo.client.features.impl.internals;

import dev.echo.client.Echo;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.InputPacketEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.BlockBreakingEvent;
import dev.echo.client.events.interaction.HandInteractEvent;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.impl.utility.AutoTotemFeature;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.items.ItemGroups;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Configurable prevention rules for attacks, mining, interaction and totem removal. */
public class PreventFeature extends Feature {
    private static BooleanSetting noAirHits;
    private static ItemListSetting preventWithItemList;
    private static BooleanSetting noMining;
    private static ItemListSetting preventWithItemList2;
    private static BooleanSetting anchorCharge;
    private static BooleanSetting eChestOpen;
    private static BooleanSetting onObsidian;
    public static BooleanSetting untotem;
    public static BooleanSetting inInventory;
    public static BooleanSetting inHotbar;
    private static BooleanSetting noShieldWithBlocks;
    private static String noAirHitsTooltip;
    private static String airItemsTooltip;
    private static String noMiningTooltip;
    private static String miningItemsTooltip;
    private static String anchorTooltip;
    private static String interactionTooltip;
    private static String untotemTooltip;
    private static String totemLocationsTooltip;

    public PreventFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Prevent"), dev.echo.client.bootstrap.CatalogText.literal("Prevents certain actions"), dev.echo.client.features.Category.INTERNALS));
    }

    @Override
    public void postInitialize() {
        preventWithItemList.setVisibilityPredicate(ignored -> noAirHits.getValue());
        preventWithItemList2.setVisibilityPredicate(ignored -> noMining.getValue());
        onObsidian.setVisibilityPredicate(ignored -> eChestOpen.getValue());
        inInventory.setVisibilityPredicate(ignored -> untotem.getValue());
        inHotbar.setVisibilityPredicate(ignored -> untotem.getValue());
        if (noAirHitsTooltip == null) noAirHitsTooltip = new String("Attacking");
        noAirHits.tooltip(noAirHitsTooltip);
        if (airItemsTooltip == null) airItemsTooltip = new String("Attacking");
        preventWithItemList.tooltip(airItemsTooltip);
        if (noMiningTooltip == null) noMiningTooltip = new String("Mining");
        noMining.tooltip(noMiningTooltip);
        if (miningItemsTooltip == null) miningItemsTooltip = new String("Mining");
        preventWithItemList2.tooltip(miningItemsTooltip);
        if (anchorTooltip == null) anchorTooltip = new String("Interact");
        anchorCharge.tooltip(anchorTooltip);
        if (interactionTooltip == null) interactionTooltip = new String("Interact");
        eChestOpen.tooltip(interactionTooltip);
        onObsidian.tooltip(interactionTooltip);
        noShieldWithBlocks.tooltip(interactionTooltip);
        if (untotemTooltip == null) untotemTooltip = new String("Totem");
        untotem.tooltip(untotemTooltip);
        if (totemLocationsTooltip == null) totemLocationsTooltip = new String("Totem");
        inInventory.tooltip(totemLocationsTooltip);
        inHotbar.tooltip(totemLocationsTooltip);
    }

    @Subscribe
    private void preventAirAttack(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.MISS) return;
        if (!noAirHits.getValue() || isAllowedItemContext(preventWithItemList)) return;
        event.cancel();
    }

    @Subscribe
    private void preventBlockAttack(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (!noMining.getValue() || isAllowedItemContext(preventWithItemList2)) return;
        event.cancel();
    }

    @Subscribe
    private void preventBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isOutsideWorld() || !noMining.getValue()) return;
        if (isAllowedItemContext(preventWithItemList2)) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        event.cancel();
    }

    @Subscribe
    private void preventExtraAnchorCharge(ItemUseEvent.Pre event) {
        if (isOutsideWorld() || !anchorCharge.getValue()) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos position = hit.getBlockPos();
            if (client.world.getBlockState(position).getBlock() == Blocks.RESPAWN_ANCHOR
                    && client.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) != 0
                    && InventoryItems.holdsInMainHand(Items.GLOWSTONE)) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void preventEnderChestOpen(ItemUseEvent.Pre event) {
        if (isOutsideWorld() || !eChestOpen.getValue()) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos position = hit.getBlockPos();
            if (client.world.getBlockState(position).getBlock() != Blocks.ENDER_CHEST) return;
            if (onObsidian.getValue()) {
                if (client.world.getBlockState(position.down()).getBlock() == Blocks.OBSIDIAN) event.cancel();
            } else {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void preventShieldUseWithBlocks(HandInteractEvent.Pre event) {
        if (isOutsideWorld() || !noShieldWithBlocks.getValue()) return;
        if (event.getHand() != Hand.OFF_HAND) return;
        if (!client.player.getOffHandStack().isOf(Items.SHIELD)) return;
        if (!(client.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        event.cancel();
    }

    @Subscribe
    private void preventTotemHotbarRemoval(InputPacketEvent event) {
        if (isOutsideWorld() || !untotem.getValue() || !inHotbar.getValue()) return;
        AutoTotemFeature autoTotem = (AutoTotemFeature) Echo.featureManager.findFeature(AutoTotemFeature.class);
        if (autoTotem != null && autoTotem.shouldAllowTotemRemoval()) return;
        if (client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) event.cancel();
    }

    private boolean isAllowedItemContext(ItemListSetting preventedItems) {
        if (preventedItems.getSelectedCount() == 0) return false;
        ItemStack stack = InventoryItems.mainHandStack();
        Item item = stack.isEmpty() ? Items.AIR : stack.getItem();
        if (preventedItems.isEntrySelected(item)) return false;
        if (ItemGroups.swords().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.swords())) return false;
        if (ItemGroups.axes().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.axes())) return false;
        if (ItemGroups.mace().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.mace())) return false;
        if (item == Items.AIR && hasSelectedItemInGroup(preventedItems, ItemGroups.fists())) return false;
        return true;
    }

    private boolean hasSelectedItemInGroup(ItemListSetting selectedItems, RegistryEntryGroup<Item> group) {
        for (Item item : Registries.ITEM) {
            if (group.matches(item) && selectedItems.isEntrySelected(item)) return true;
        }
        return false;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(PreventFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.noAirHits);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.preventWithItemList);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.noMining);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.preventWithItemList2);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.anchorCharge);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.eChestOpen);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.onObsidian);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.untotem);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.inInventory);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.inHotbar);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, PreventFeature.noShieldWithBlocks);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(PreventFeature feature, dev.echo.client.setting.ItemListSetting preparedPreventWithItemList, dev.echo.client.setting.BooleanSetting preparedNoAirHits, dev.echo.client.setting.BooleanSetting preparedNoShieldWithBlocks, dev.echo.client.setting.BooleanSetting preparedInHotbar, dev.echo.client.setting.BooleanSetting preparedUntotem, dev.echo.client.setting.BooleanSetting preparedOnObsidian, dev.echo.client.setting.ItemListSetting preparedPreventWithItemList2, dev.echo.client.setting.BooleanSetting preparedNoMining, dev.echo.client.setting.BooleanSetting preparedEChestOpen, dev.echo.client.setting.BooleanSetting preparedInInventory, dev.echo.client.setting.BooleanSetting preparedAnchorCharge) {
        PreventFeature.preventWithItemList = preparedPreventWithItemList;
        PreventFeature.noAirHits = preparedNoAirHits;
        PreventFeature.noShieldWithBlocks = preparedNoShieldWithBlocks;
        PreventFeature.inHotbar = preparedInHotbar;
        PreventFeature.untotem = preparedUntotem;
        PreventFeature.onObsidian = preparedOnObsidian;
        PreventFeature.preventWithItemList2 = preparedPreventWithItemList2;
        PreventFeature.noMining = preparedNoMining;
        PreventFeature.eChestOpen = preparedEChestOpen;
        PreventFeature.inInventory = preparedInInventory;
        PreventFeature.anchorCharge = preparedAnchorCharge;
    }
}
