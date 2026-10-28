package dev.echo.client.features.impl.utility;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HotbarKeyEvent;
import dev.echo.client.events.input.UseKeyEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.items.InventoryItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;

/** Hotbar-mediated chestplate/elytra switching with the original two-tick selection window. */
public class ElytraSwapFeature extends Feature {
    private static BooleanSetting switchBack;
    private static KeybindSetting activateKey;
    private boolean swapActive;
    private boolean startedWithElytra;
    private boolean activateWasPressed;
    private int pendingHotbarSlot;
    private long pendingSlotPlayerAge;

    public ElytraSwapFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Elytra Swap"), dev.echo.client.bootstrap.CatalogText.literal("Quick elytra swap"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        swapActive = false;
        startedWithElytra = false;
        activateWasPressed = false;
        pendingHotbarSlot = -1;
        pendingSlotPlayerAge = -1L;
        super.postInitialize();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (isOutsideWorld() || client.player.isCreative()) {
            return;
        }
        try {
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        boolean pressed = activateKey.getKey().isPressed();
        if (pressed && !activateWasPressed && !swapActive) {
            beginSwap();
        }
        activateWasPressed = pressed;
        if (!swapActive) {
            return;
        }
        if (InventoryItems.wearsElytra() != startedWithElytra) {
            DeferredActionQueue.finish(this, switchBack.getValue());
            resetSwapState();
            return;
        }
        if (holdsDesiredItem()) {
            pendingHotbarSlot = -1;
            event.addClick();
            return;
        }
        if (pendingHotbarSlot != -1) {
            return;
        }
        int slot = findDesiredHotbarSlot();
        if (slot == -1) {
            DeferredActionQueue.finish(this, false);
            resetSwapState();
            return;
        }
        pendingHotbarSlot = slot;
        pendingSlotPlayerAge = client.player.age;
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (pendingHotbarSlot == -1 || event.slot != pendingHotbarSlot) {
            return;
        }
        if (client.player == null || (long) client.player.age - pendingSlotPlayerAge > 2L) {
            pendingHotbarSlot = -1;
            pendingSlotPlayerAge = -1L;
            return;
        }
        pendingHotbarSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != event.slot) {
            if (!DeferredActionQueue.select(this, event.slot, -1, false)) {
                DeferredActionQueue.finish(this, false);
                resetSwapState();
                return;
            }
        }
        event.addClick();
    }

    private boolean holdsDesiredItem() {
        if (startedWithElytra) {
            return InventoryItems.holdsChestplateInMainHand();
        }
        return client.player.getMainHandStack().getItem() == Items.ELYTRA;
    }

    private int findDesiredHotbarSlot() {
        if (startedWithElytra) {
            return InventoryItems.findChestplateInHotbar();
        }
        return InventoryItems.findStackInHotbar(ElytraSwapItems::isElytra);
    }

    private void beginSwap() {
        swapActive = true;
        startedWithElytra = InventoryItems.wearsElytra();
    }

    private void resetSwapState() {
        swapActive = false;
        startedWithElytra = false;
        pendingHotbarSlot = -1;
        pendingSlotPlayerAge = -1L;
    }

    @Override
    public void onEnable() {
        resetSwapState();
        activateWasPressed = false;
        DeferredActionQueue.finish(this, false);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetSwapState();
        activateWasPressed = false;
        DeferredActionQueue.finish(this, false);
        super.onDisable();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(ElytraSwapFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ElytraSwapFeature.switchBack);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ElytraSwapFeature.activateKey);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(ElytraSwapFeature feature, dev.echo.client.setting.BooleanSetting preparedSwitchBack, dev.echo.client.setting.KeybindSetting preparedActivateKey) {
        ElytraSwapFeature.switchBack = preparedSwitchBack;
        ElytraSwapFeature.activateKey = preparedActivateKey;
    }
}
