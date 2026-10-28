package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.PiercingFeature;

/** Constructs, assigns, collects and post-initializes Piercing. */
public final class PiercingDefaults {
    private PiercingDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PiercingFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedSortTargetsBy = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Sort Targets By"), CatalogText.literal("Hurt Time"), new java.lang.CharSequence[] {CatalogText.literal("Hurt Time"), CatalogText.literal("Health")});
        dev.echo.client.setting.BooleanSetting preparedBlocks = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Blocks"), true);
        dev.echo.client.setting.BooleanSetting preparedInteract = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Interact"), false);

        PiercingFeature.sortTargetsBy = preparedSortTargetsBy;
        PiercingFeature.blocks = preparedBlocks;
        PiercingFeature.interact = preparedInteract;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.combat.PiercingFeature.blocks);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.combat.PiercingFeature.interact);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.combat.PiercingFeature.sortTargetsBy);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.combat.PiercingFeature.holdingBlock);
        feature.postInitialize();
    }
}
