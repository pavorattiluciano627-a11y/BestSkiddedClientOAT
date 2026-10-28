package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.ChestStealerFeature;

/** Constructs, assigns, collects and post-initializes Chest Stealer. */
public final class ChestStealerDefaults {
    private ChestStealerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ChestStealerFeature feature) {
        dev.echo.client.setting.IntSetting preparedInitialDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Initial Delay"), 150, 1, 1000, CatalogText.literal(" ms"));
        dev.echo.client.setting.IntSetting preparedTakeDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Take Delay"), 70, 1, 1000, CatalogText.literal(" ms"));
        dev.echo.client.setting.IntSetting preparedCloseDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Close Delay"), 50, 1, 1000, CatalogText.literal(" ms"));
        dev.echo.client.setting.BooleanSetting preparedOnlyImportant = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Only Important"), false);

        ChestStealerFeature.initialDelay = preparedInitialDelay;
        ChestStealerFeature.takeDelay = preparedTakeDelay;
        ChestStealerFeature.closeDelay = preparedCloseDelay;
        ChestStealerFeature.onlyImportant = preparedOnlyImportant;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.world.ChestStealerFeature.initialDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.world.ChestStealerFeature.takeDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.world.ChestStealerFeature.closeDelay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.world.ChestStealerFeature.onlyImportant);
        feature.postInitialize();
    }
}
