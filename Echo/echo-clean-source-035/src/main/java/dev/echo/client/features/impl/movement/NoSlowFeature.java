package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.movement.MovementSpeedEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.strings.Concat;

/** Item-use slowdown control with the original tick-scoped Grim delay state. */
public class NoSlowFeature extends Feature {
    private static final CharSequence GRIM_MODE = Concat.strings("Grim".split(""));
    private static ChoiceSetting mode;
    private static BooleanSetting enableSneak;
    private static IntSetting delayTicks;
    private int itemUseDelayTicks;
    private boolean slowdownCancelledThisTick;

    public NoSlowFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("No Slow"), dev.echo.client.bootstrap.CatalogText.literal("Removes slow effect from items"), dev.echo.client.features.Category.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
        delayTicks.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_MODE));
        delayTicks.tooltip("Timing");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player == null) {
            return;
        }
        slowdownCancelledThisTick = false;
        if (!client.player.isUsingItem()) {
            itemUseDelayTicks = 0;
        } else {
            itemUseDelayTicks++;
        }
    }

    @Subscribe
    public void onMovementSpeed(MovementSpeedEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (mode.isSelected(GRIM_MODE)) {
            if (slowdownCancelledThisTick) {
                event.setHasVehicle(true);
                return;
            }
            if (itemUseDelayTicks >= delayTicks.getValue()) {
                slowdownCancelledThisTick = true;
                itemUseDelayTicks = 0;
                event.setHasVehicle(true);
            }
            return;
        }
        event.setActiveItemSpeedMultiplier(1.0f);
        event.setBlockedFromSprinting(false);
        if (enableSneak.getValue()) {
            event.setSlowDown(false);
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(NoSlowFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, NoSlowFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, NoSlowFeature.enableSneak);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, NoSlowFeature.delayTicks);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(NoSlowFeature feature, dev.echo.client.setting.IntSetting preparedDelayTicks, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.BooleanSetting preparedEnableSneak) {
        NoSlowFeature.delayTicks = preparedDelayTicks;
        NoSlowFeature.mode = preparedMode;
        NoSlowFeature.enableSneak = preparedEnableSneak;
    }
}
