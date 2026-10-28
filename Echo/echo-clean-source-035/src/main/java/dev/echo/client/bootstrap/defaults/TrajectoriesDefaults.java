package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.TrajectoriesFeature;

/** Constructs, assigns, collects and post-initializes Trajectories. */
public final class TrajectoriesDefaults {
    private TrajectoriesDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TrajectoriesFeature feature) {
        dev.echo.client.setting.ColorSetting preparedArrowColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Arrow Color"), 255, 128, 128, 200, true);
        dev.echo.client.setting.ColorSetting preparedPearlColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Pearl Color"), 102, 179, 255, 200, true);
        dev.echo.client.setting.ColorSetting preparedWindChargeColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Wind Charge Color"), 255, 209, 102, 200, true);
        dev.echo.client.setting.ColorSetting preparedPotionColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Potion Color"), 124, 192, 255, 200, true);
        dev.echo.client.setting.ColorSetting preparedFireworkColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Firework Color"), 255, 196, 130, 200, true);
        dev.echo.client.setting.ColorSetting preparedBowColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Bow Color"), 148, 114, 255, 200, true);
        dev.echo.client.setting.ColorSetting preparedCrossbowColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Crossbow Color"), 114, 216, 180, 200, true);
        dev.echo.client.setting.MultiChoiceSetting preparedTrajectories = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Trajectories"), new java.lang.CharSequence[] {dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption2(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption5(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption3(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption4(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption7(), dev.echo.client.features.impl.render.TrajectoriesFeature.bootstrapTrajectoriesOption6()});

        TrajectoriesFeature.assignBootstrapSettings(feature, preparedArrowColor, preparedPearlColor, preparedWindChargeColor, preparedPotionColor, preparedFireworkColor, preparedBowColor, preparedCrossbowColor, preparedTrajectories);

        feature.settings.clear();
        dev.echo.client.features.impl.render.TrajectoriesFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
