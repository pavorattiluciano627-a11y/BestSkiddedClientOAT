package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AutoCartFeature;

/** Constructs, assigns, collects and post-initializes Auto Cart. */
public final class AutoCartDefaults {
    private AutoCartDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoCartFeature feature) {
        dev.echo.client.setting.ItemListSetting preparedReturnItems = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Return Items"));
        dev.echo.client.setting.ChoiceSetting preparedReturnTo = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Return To"), dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapReturnToOption3(), dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapReturnToOption2(), dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapReturnToOption()});
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        dev.echo.client.setting.BooleanSetting preparedFreshArrowsOnly = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Fresh Arrows Only"), true);
        dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.echo.client.setting.MultiChoiceSetting preparedPlace = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapPlaceOption(), dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapPlaceOption2(), dev.echo.client.features.impl.combat.AutoCartFeature.bootstrapPlaceOption3()});

        AutoCartFeature.assignBootstrapSettings(feature, preparedReturnItems, preparedReturnTo, preparedSilentRotations, preparedFreshArrowsOnly, preparedReturnSlots, preparedPlace);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AutoCartFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
