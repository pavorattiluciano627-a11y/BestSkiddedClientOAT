package dev.echo.client.features.impl.utility;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.input.SelectedSlotEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.DeferredActionQueue;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.input.InputKey;
import dev.echo.client.mixin.KeyBindingAccessor;
import dev.echo.client.mixin.MinecraftClientAccessor;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.utils.ActionTimer;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.InventoryItems;
import dev.echo.client.utils.targeting.CrosshairHits;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Timed pearl selection/use/restoration with optional visible-rotation synchronization. */
public class KeyPearlFeature extends Feature {
    private static BooleanSetting swapBack;
    private static FloatRangeSetting swapDelay;
    private static FloatRangeSetting throwDelay;
    private static KeybindSetting activateKey;
    private static BooleanSetting syncToClient;
    int pearlSlot;
    int slotBeforeThrow;
    int lastPreviousSlot;
    private ActionTimer phaseTimer;
    private boolean throwInProgress;
    private boolean pearlThrown;
    private boolean activateWasPressed;
    private int sampledThrowDelay;
    private int sampledSwapDelay;

    public KeyPearlFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Key Pearl"), dev.echo.client.bootstrap.CatalogText.literal("Quick pearl throw"), dev.echo.client.features.Category.UTILITY));
    }

    @Override
    public void postInitialize() {
        pearlSlot = -1;
        slotBeforeThrow = -1;
        lastPreviousSlot = -1;
        phaseTimer = new ActionTimer();
        throwInProgress = false;
        pearlThrown = false;
        activateWasPressed = false;
        sampledThrowDelay = 0;
        sampledSwapDelay = 0;
        swapDelay.setVisibilityPredicate(ignored -> swapBack.getValue());
        swapDelay.tooltip("Timing");
        throwDelay.tooltip("Timing");
    }

    @Subscribe
    public void onSelectedSlot(SelectedSlotEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (!throwInProgress) {
            lastPreviousSlot = event.getPreviousSlot();
        }
    }

    @Subscribe
    public void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) {
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
        if (pressed && !activateWasPressed) {
            slotBeforeThrow = client.player.getInventory().getSelectedSlot();
            pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() instanceof EnderPearlItem);
            throwInProgress = false;
            pearlThrown = false;
            sampledThrowDelay = sampleDelay(throwDelay);
            phaseTimer.reset();
            if (pearlSlot != -1) {
                InputKey key = activateKey.getKey();
                if (!key.isMouse() && client.options != null) {
                    for (int slot = 0; slot < 9; slot++) {
                        KeyBindingAccessor binding = (KeyBindingAccessor) client.options.hotbarKeys[slot];
                        InputUtil.Key boundKey = binding.getBoundKey();
                        if (boundKey != null && key.matches(boundKey.getCode())) {
                            binding.setTimesPressed(0);
                            break;
                        }
                    }
                }
                if (!client.player.getItemCooldownManager().isCoolingDown(createPearlIcon())) {
                    throwInProgress = true;
                    if (swapBack.getValue()) {
                        if (!DeferredActionQueue.ensureSelected(this, pearlSlot, -1, true, null)) {
                            throwInProgress = false;
                            return;
                        }
                    } else {
                        InventoryItems.selectHotbarSlot(pearlSlot);
                    }
                }
            }
        }
        if (throwInProgress && !pearlThrown && phaseTimer.hasElapsedTicks(sampledThrowDelay)) {
            Runnable throwAction = KeyPearlFeature::throwSelectedPearl;
            if (syncToClient.getValue()) {
                Rotation rotation = rotations().snapshot().playerRotation();
                rotations().override(this, RotationPriority.CRITICAL, rotation).ifPresentOrElse(scope -> {
                    try (scope) {
                        throwAction.run();
                    }
                }, throwAction::run);
            } else {
                throwAction.run();
            }
            pearlThrown = true;
            sampledSwapDelay = sampleDelay(swapDelay);
            phaseTimer.reset();
        }
        if (pearlThrown && swapBack.getValue() && phaseTimer.hasElapsedTicks(sampledSwapDelay)) {
            DeferredActionQueue.finish(this);
            throwInProgress = false;
            pearlThrown = false;
        }
        activateWasPressed = pressed;
    }

    private static int sampleDelay(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private ItemStack createPearlIcon() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    private static void throwSelectedPearl() {
        CrosshairHits.withItemUseHit(KeyPearlFeature::useSelectedItem);
    }

    private static void useSelectedItem() {
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        ((MinecraftClientAccessor) (Object) client).invokeDoItemUse();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        super.onDisable();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(KeyPearlFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeyPearlFeature.swapBack);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeyPearlFeature.swapDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeyPearlFeature.throwDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeyPearlFeature.activateKey);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeyPearlFeature.syncToClient);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(KeyPearlFeature feature, dev.echo.client.setting.BooleanSetting preparedSyncToClient, dev.echo.client.setting.FloatRangeSetting preparedThrowDelay, dev.echo.client.setting.KeybindSetting preparedActivateKey, dev.echo.client.setting.BooleanSetting preparedSwapBack, dev.echo.client.setting.FloatRangeSetting preparedSwapDelay) {
        KeyPearlFeature.syncToClient = preparedSyncToClient;
        KeyPearlFeature.throwDelay = preparedThrowDelay;
        KeyPearlFeature.activateKey = preparedActivateKey;
        KeyPearlFeature.swapBack = preparedSwapBack;
        KeyPearlFeature.swapDelay = preparedSwapDelay;
    }
}
