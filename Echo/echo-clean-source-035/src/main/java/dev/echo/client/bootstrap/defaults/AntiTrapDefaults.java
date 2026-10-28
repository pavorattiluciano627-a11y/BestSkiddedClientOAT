package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.AntiTrapFeature;

/** Constructs, assigns, collects and post-initializes Anti Trap. */
public final class AntiTrapDefaults {
    private AntiTrapDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AntiTrapFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSigns = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Signs"), true);
        dev.echo.client.setting.BooleanSetting preparedHangingSigns = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Hanging Signs"), true);
        dev.echo.client.setting.BooleanSetting preparedGlowFrames = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Glow Frames"), true);
        dev.echo.client.setting.BooleanSetting preparedPaintings = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Paintings"), true);
        dev.echo.client.setting.BooleanSetting preparedChestMinecarts = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Chest Minecarts"), true);
        dev.echo.client.setting.BooleanSetting preparedArmorStands = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Armor Stands"), true);
        dev.echo.client.setting.BooleanSetting preparedNoInteract = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Interact"), true);
        dev.echo.client.setting.BooleanSetting preparedItemFrames = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Item Frames"), true);

        AntiTrapFeature.assignBootstrapSettings(feature, preparedSigns, preparedHangingSigns, preparedGlowFrames, preparedPaintings, preparedChestMinecarts, preparedArmorStands, preparedNoInteract, preparedItemFrames);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.AntiTrapFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
