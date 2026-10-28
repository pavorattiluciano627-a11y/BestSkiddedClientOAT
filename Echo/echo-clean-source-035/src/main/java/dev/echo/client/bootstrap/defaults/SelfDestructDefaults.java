package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.internals.SelfDestructFeature;

/** Constructs, assigns, collects and post-initializes Self Destruct. */
public final class SelfDestructDefaults {
    private SelfDestructDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SelfDestructFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedReplaceMods = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Replace Mods"), false);
        dev.echo.client.setting.TextSetting preparedUrl = new dev.echo.client.setting.TextSetting(CatalogText.literal("URL"), new String("https://cdn.modrinth.com/data/lIQY64qr/versions/DTs0nTbx/tiptapshow-2.0.0%2B1.21.11.jar"));

        SelfDestructFeature.assignBootstrapSettings(feature, preparedReplaceMods, preparedUrl);

        feature.settings.clear();
        dev.echo.client.features.impl.internals.SelfDestructFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
