package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.XrayFeature;

/** Constructs, assigns, collects and post-initializes Xray. */
public final class XrayDefaults {
    private XrayDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(XrayFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedThroughLiquids = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Through Liquids"), true);
        dev.echo.client.setting.BooleanSetting preparedExposedToAir = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Exposed to Air"), false);

        XrayFeature.throughLiquids = preparedThroughLiquids;
        XrayFeature.exposedToAir = preparedExposedToAir;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.XrayFeature.throughLiquids);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.XrayFeature.exposedToAir);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.XrayFeature.blocks);
        feature.postInitialize();
    }
}
