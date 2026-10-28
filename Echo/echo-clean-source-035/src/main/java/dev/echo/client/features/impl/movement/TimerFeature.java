package dev.echo.client.features.impl.movement;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.strings.Concat;

/** Applies the live configured tick multiplier while enabled. */
public class TimerFeature extends Feature {
    public static FloatSetting timer;

    public TimerFeature() {
        super(new FeatureInfo(CatalogText.literal("Timer"), CatalogText.literal("Modifies game tick speed"),
            Category.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        timer.setAllowOutOfRange(true);
    }

    @Override
    public CharSequence getSuffix() {
        return Concat.formattedFloat(timer.getValue(), 2);
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || !isEnabled()) {
            return;
        }
        MinecraftAccess.setTickSpeed(timer.getValue());
    }

    /** The original native override resets the speed without calling the base hook. */
    @Override
    public void onDisable() {
        MinecraftAccess.setTickSpeed(1.0f);
    }

    public static void appendBootstrapSettings(TimerFeature feature) {
        CatalogSettings.append(feature, timer);
    }

    public static void assignBootstrapSettings(TimerFeature feature, FloatSetting preparedTimer) {
        timer = preparedTimer;
    }
}
