package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.internals.ProtectFeature;

/** Constructs, assigns, collects and post-initializes Protect. */
public final class ProtectDefaults {
    private ProtectDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ProtectFeature feature) {
        dev.echo.client.setting.TextSetting preparedSkin = new dev.echo.client.setting.TextSetting(CatalogText.literal("Skin"), new String("Notch"));
        dev.echo.client.setting.TextSetting preparedNickname = new dev.echo.client.setting.TextSetting(CatalogText.literal("Nickname"), new String("Player"));

        ProtectFeature.skin = preparedSkin;
        ProtectFeature.nickname = preparedNickname;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.internals.ProtectFeature.nickname);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.internals.ProtectFeature.skin);
        feature.postInitialize();
    }
}
