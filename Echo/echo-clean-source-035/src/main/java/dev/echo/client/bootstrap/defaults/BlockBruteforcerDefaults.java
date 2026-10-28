package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.BlockBruteforcerFeature;

/** Constructs, assigns, collects and post-initializes Block Bruteforcer. */
public final class BlockBruteforcerDefaults {
    private BlockBruteforcerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(BlockBruteforcerFeature feature) {
        dev.echo.client.setting.IntSetting preparedRadius = new dev.echo.client.setting.IntSetting(CatalogText.literal("Radius"), 5, 1, 8, CatalogText.literal(" blocks"));
        dev.echo.client.setting.IntSetting preparedUp = new dev.echo.client.setting.IntSetting(CatalogText.literal("Up"), 3, 0, 64, CatalogText.literal(" blocks"));
        dev.echo.client.setting.IntSetting preparedDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
        dev.echo.client.setting.IntSetting preparedDown = new dev.echo.client.setting.IntSetting(CatalogText.literal("Down"), 3, 0, 64, CatalogText.literal(" blocks"));
        dev.echo.client.setting.IntSetting preparedBlocksPerTick = new dev.echo.client.setting.IntSetting(CatalogText.literal("Blocks Per Tick"), 4, 1, 5, CatalogText.literal(" blocks"));
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Abort"), new java.lang.CharSequence[] {CatalogText.literal("Abort"), CatalogText.literal("Start")});
        dev.echo.client.setting.BooleanSetting preparedFreezeMovement = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Freeze Movement"), true);

        BlockBruteforcerFeature.assignBootstrapSettings(feature, preparedRadius, preparedUp, preparedDelay, preparedDown, preparedBlocksPerTick, preparedMode, preparedFreezeMovement);

        feature.settings.clear();
        dev.echo.client.features.impl.world.BlockBruteforcerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
