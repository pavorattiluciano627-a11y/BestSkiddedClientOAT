package dev.echo.client.features.impl.combat;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.AttackKeyEvent;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

/** Totem-to-knockback-sword attack substitution; conditional recovery domains are in receipt.json. */
public class TotemHitFeature extends Feature {
    public static BooleanSetting silentSwap;
    public static FloatRangeSetting delay;
    boolean hotbarActionActive;
    private int knockbackSwordSlot;
    private boolean swordSwapPending;
    private long lastSwordSwapTick;
    private static final String TIMING_SECTION = "Timing";

    public TotemHitFeature() {
        this(new FeatureInfo(literal("Totem Hit"), literal("Swaps to knockback sword when attacking with totem"), Category.COMBAT));
    }

    public TotemHitFeature(FeatureInfo info) {
        super(info);
    }

    @Override
    public void postInitialize() {
        hotbarActionActive = false;
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
        delay.setVisibilityPredicate(ignored -> !silentSwap.getValue());
        delay.tooltip(TIMING_SECTION);
    }

    private int findKnockbackSword() {
        return InventoryItems.findItemWithEnchantmentInHotbar(
            stack -> InventoryItems.isLegacySword(stack.getItem()), Enchantments.KNOCKBACK, false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hotbarActionActive = false;
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
        DeferredActionQueue.finish(this, true);
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (knockbackSwordSlot == -1 || event.slot != knockbackSwordSlot) return;
        if (client.player == null || (long) client.player.age - lastSwordSwapTick > 2L) {
            resetSwordSwap();
            return;
        }
        knockbackSwordSlot = -1;
        int delayTicks = silentSwap.getValue() ? 0 : (int) delay.sampleGaussian();
        boolean restoreAll = !silentSwap.getValue();
        if (!DeferredActionQueue.select(this, event.slot, delayTicks, restoreAll,
                () -> hotbarActionActive = false)) {
            resetSwordSwap();
            return;
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOW)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || client.isPaused()) return;
        if (swordSwapPending) {
            boolean recent = (long) client.player.age - lastSwordSwapTick <= 2L;
            resetSwordSwap();
            if (recent && InventoryItems.isLegacySword(client.player.getMainHandStack().getItem())) {
                if (!event.hasClicks()) event.addClick();
                hotbarActionActive = true;
            }
            return;
        }
        if (!event.hasClicks() || client.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (!TargetSelection.isValidTarget(target)) return;
        int slot = findKnockbackSword();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        event.setClickCount(0);
        knockbackSwordSlot = slot;
        swordSwapPending = true;
        lastSwordSwapTick = client.player.age;
    }

    private void resetSwordSwap() {
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
    }

    public static void appendBootstrapSettings(TotemHitFeature feature) {
        CatalogSettings.append(feature, silentSwap);
        CatalogSettings.append(feature, delay);
    }

    public static void assignBootstrapSettings(TotemHitFeature feature, FloatRangeSetting preparedDelay,
            BooleanSetting preparedSilentSwap) {
        delay = preparedDelay;
        silentSwap = preparedSilentSwap;
    }
}
