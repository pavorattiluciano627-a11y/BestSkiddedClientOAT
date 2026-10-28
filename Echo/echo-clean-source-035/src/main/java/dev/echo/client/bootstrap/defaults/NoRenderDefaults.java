package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.NoRenderFeature;

/** Constructs, assigns, collects and post-initializes No Render. */
public final class NoRenderDefaults {
    private NoRenderDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NoRenderFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedNoSpyglassOverlay = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Spyglass Overlay"), false);
        dev.echo.client.setting.BooleanSetting preparedNoNausea = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Nausea"), false);
        dev.echo.client.setting.BooleanSetting preparedNoPortalOverlay = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Portal Overlay"), false);
        dev.echo.client.setting.BooleanSetting preparedNoFireOverlay = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Fire Overlay"), false);
        dev.echo.client.setting.BooleanSetting preparedNoFloatingItem = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Floating Item"), false);
        dev.echo.client.setting.BooleanSetting preparedNoVignette = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Vignette"), false);
        dev.echo.client.setting.BooleanSetting preparedNoCrystalBounce = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Crystal Bounce"), false);

        NoRenderFeature.noSpyglassOverlay = preparedNoSpyglassOverlay;
        NoRenderFeature.noNausea = preparedNoNausea;
        NoRenderFeature.noPortalOverlay = preparedNoPortalOverlay;
        NoRenderFeature.noFireOverlay = preparedNoFireOverlay;
        NoRenderFeature.noFloatingItem = preparedNoFloatingItem;
        NoRenderFeature.noVignette = preparedNoVignette;
        NoRenderFeature.noCrystalBounce = preparedNoCrystalBounce;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noFireOverlay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noNausea);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noVignette);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noFloatingItem);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noSpyglassOverlay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noPortalOverlay);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.NoRenderFeature.noCrystalBounce);
        feature.postInitialize();
    }
}
