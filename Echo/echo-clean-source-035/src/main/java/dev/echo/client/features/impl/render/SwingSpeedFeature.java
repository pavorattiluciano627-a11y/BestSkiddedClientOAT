package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.IntSetting;

/** Candidate; defaults are metadata-proven, tooltip dispatch is conditional. */
public class SwingSpeedFeature extends Feature {
    public static IntSetting swingSpeed;

    public SwingSpeedFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Swing Speed"), dev.echo.client.bootstrap.CatalogText.literal("Increases the speed of your swing animation"), dev.echo.client.features.Category.RENDER, false));
    }

    @Override
    public void postInitialize() {
        swingSpeed.tooltip("Timing");
    }

}
