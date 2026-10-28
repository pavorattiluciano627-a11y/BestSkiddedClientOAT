package dev.echo.client.features.impl.combat;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.HandSwingEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.HotbarSlotsSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.items.ItemProperties;
import dev.echo.client.utils.targeting.EntityRaycasts;
import dev.echo.client.utils.targeting.WeaponAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;

/** Temporarily selects a non-Lunge spear and performs its longer-range attack. */
public class SpearReachFeature extends Feature {
    private static BooleanSetting paperMCSupport;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static final int SWAP_DURATION_TICKS = 1;
    private ActionTimer swapTimer;
    private boolean awaitingRestore;
    private static String[] sharedControlState;

    static {
        writeSharedControlState(null);
    }

    public SpearReachFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Spear Reach"), dev.echo.client.bootstrap.CatalogText.literal("Swaps to spears for increased reach"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        swapTimer = new ActionTimer();
        awaitingRestore = false;
        itemWhitelist.tooltip("Conditions");
        hotbarWhitelist.tooltip("Conditions");
    }

    private int findSpearSlot() {
        try {
            Class.forName(Minecraft.class.getName(), true, Minecraft.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (client.player == null) return -1;
        return InventoryItems.findStackInHotbar(SpearReachFeature::isSelectableSpear);
    }

    private static boolean isSelectableSpear(ItemStack stack) {
        String[] controlState = readSharedControlState();
        if (controlState == null && stack.isEmpty()) return false;
        try {
            Class.forName(Item.class.getName(), true, Item.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (!InventoryItems.isSpear(stack.getItem())) return false;
        try {
            Class.forName(ItemStack.class.getName(), true, ItemStack.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        try {
            Class.forName(ResourceKey.class.getName(), true, ResourceKey.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        boolean hasLunge = ItemProperties.hasEnchantment(stack, Enchantments.LUNGE);
        return controlState != null ? hasLunge : !hasLunge;
    }

    private boolean isHoldingSelectableSpear() {
        String[] controlState = readSharedControlState();
        if (controlState == null && client.player == null) return false;
        ItemStack stack = client.player.getMainHandItem();
        if (controlState == null && stack.isEmpty()) return false;
        if (!InventoryItems.isSpear(stack.getItem())) return false;
        boolean hasLunge = ItemProperties.hasEnchantment(stack, Enchantments.LUNGE);
        return controlState != null ? hasLunge : !hasLunge;
    }

    @Subscribe
    public final void onAttackAttempt(AttackAttemptEvent event) {
        String[] controlState = readSharedControlState();
        if (controlState == null && findExtendedReachTarget() == null) return;
        int spearSlot = findSpearSlot();
        if (controlState == null) {
            if (spearSlot == -1) return;
            int selectedSlot = client.player.getInventory().getSelectedSlot();
            if (selectedSlot == spearSlot) return;
            if (!DeferredActionQueue.select(this, spearSlot, SWAP_DURATION_TICKS, true,
                    () -> awaitingRestore = false)) return;
        } else if (spearSlot == 0) {
            return;
        }
        swapTimer.reset();
        awaitingRestore = true;
        event.cancel();
        WeaponAttributes.performPiercingAttack();
    }

    @Subscribe
    public void onHandSwing(HandSwingEvent event) {
        if (paperMCSupport.getValue() && event.getPlayer() == client.player) {
            WeaponAttributes.recordSwingTick(client.player);
        }
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        String[] controlState = readSharedControlState();
        if (controlState == null) {
            if (isOutsideWorld()) return;
            if (!awaitingRestore) return;
            if (!swapTimer.hasElapsedTicks(SWAP_DURATION_TICKS)) return;
            DeferredActionQueue.finish(this);
        }
        awaitingRestore = false;
    }

    /** Stack used by Paper's cooldown calculation when a spear swap is possible. */
    public ItemStack getReachStack() {
        String[] controlState = readSharedControlState();
        boolean gate = isEnabled();
        if (controlState == null) {
            if (!gate) return ItemStack.EMPTY;
            gate = paperMCSupport.getValue();
            if (!gate) return ItemStack.EMPTY;
            gate = isOutsideWorld();
        }
        if (gate) return ItemStack.EMPTY;
        int slot = findSpearSlot();
        return slot == -1 ? ItemStack.EMPTY : client.player.getInventory().getItem(slot);
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        awaitingRestore = false;
    }

    public EntityHitResult findExtendedReachTarget() {
        String[] controlState = readSharedControlState();
        if (controlState == null && !isEnabled()) return null;
        if (isOutsideWorld()) return null;
        if (Minecraft.getInstance().gui.screen() != null) return null;
        boolean gate = client.isPaused();
        int slot;
        if (controlState == null) {
            if (gate) return null;
            if (isHoldingSelectableSpear()) return null;
            if (!itemWhitelist.acceptsHeldItem()) return null;
            if (!hotbarWhitelist.permitsCurrentSlot()) return null;
            if (EntityRaycasts.hasNormalReachTarget(client.player)) return null;
            slot = findSpearSlot();
        } else {
            slot = gate ? 1 : 0;
        }
        if (slot == -1) return null;
        ItemStack spear = client.player.getInventory().getItem(slot);
        EntityHitResult hit = EntityRaycasts.raycastWithStackRange(client.player, spear);
        if (hit == null) return null;
        boolean paperSupport = paperMCSupport.getValue();
        if (controlState != null) {
            if (paperSupport) return null;
        } else if (paperSupport
                && WeaponAttributes.remainingCooldownSeconds(client.player, spear, 1.0f) > 0.0) {
            return null;
        }
        return hit;
    }

    public static boolean hasPendingSwap() {
        try {
            Class.forName(Class.class.getName(), true, Class.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        return DeferredActionQueue.contains(SpearReachFeature.class);
    }

    /** Original shared array identity is observable by the other feature owners. */
    public static String[] readSharedControlState() { return sharedControlState; }
    public static void writeSharedControlState(String[] value) { sharedControlState = value; }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SpearReachFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearReachFeature.paperMCSupport);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearReachFeature.itemWhitelist);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpearReachFeature.hotbarWhitelist);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SpearReachFeature feature, dev.echo.client.setting.BooleanSetting preparedPaperMCSupport, dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist, dev.echo.client.setting.ItemListSetting preparedItemWhitelist) {
        SpearReachFeature.paperMCSupport = preparedPaperMCSupport;
        SpearReachFeature.hotbarWhitelist = preparedHotbarWhitelist;
        SpearReachFeature.itemWhitelist = preparedItemWhitelist;
    }
}
