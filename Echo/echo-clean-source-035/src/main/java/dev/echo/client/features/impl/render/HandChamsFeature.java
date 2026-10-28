package dev.echo.client.features.impl.render;

import dev.echo.client.features.Feature;
import dev.echo.client.render.EntityMaterialSettings;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.render.HandChamsRenderer;

/** Hand material settings and its continuous, speed-scaled animation phase. */
public class HandChamsFeature extends Feature implements EntityMaterialSettings {
    private static ColorSetting color1;
    private static ColorSetting color2;
    private static FloatSetting lineWidth;
    private static FloatSetting outlineOpacity;
    private static FloatSetting fillOpacity;
    private static FloatSetting glowRadius;
    private static FloatSetting glowIntensity;
    private static FloatSetting speed;
    private static FloatSetting gradientScale;
    private float animationPhase;
    private long previousAnimationNanos;

    public HandChamsFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Hand Chams"), dev.echo.client.bootstrap.CatalogText.literal("Gradient outline shader on your hand"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        color1.tooltip("Appearance");
        color2.tooltip("Appearance");
        lineWidth.tooltip("Appearance");
        outlineOpacity.tooltip("Appearance");
        fillOpacity.tooltip("Appearance");
        glowRadius.tooltip("Appearance");
        glowIntensity.tooltip("Appearance");
        speed.tooltip("Timing");
        gradientScale.tooltip("Appearance");
    }

    @Override
    public ColorSetting getColor1Setting() {
        return color1;
    }

    @Override
    public ColorSetting getColor2Setting() {
        return color2;
    }

    @Override
    public FloatSetting getLineWidthSetting() {
        return lineWidth;
    }

    @Override
    public FloatSetting getOutlineOpacitySetting() {
        return outlineOpacity;
    }

    @Override
    public FloatSetting getFillOpacitySetting() {
        return fillOpacity;
    }

    @Override
    public FloatSetting getGlowRadiusSetting() {
        return glowRadius;
    }

    @Override
    public FloatSetting getGlowIntensitySetting() {
        return glowIntensity;
    }

    @Override
    public FloatSetting getGradientScaleSetting() {
        return gradientScale;
    }

    @Override
    public float getAnimationPhase() {
        boolean paused = FreecamFeature.isPerspectiveRefreshBypassed();
        long now = System.nanoTime();
        if (paused) {
            return animationPhase;
        }
        if (previousAnimationNanos != 0L) {
            float elapsedSeconds = (float) (now - previousAnimationNanos) / 1.0E9f;
            animationPhase = (animationPhase + elapsedSeconds * speed.getValue()) % 1.0f;
        }
        previousAnimationNanos = now;
        return animationPhase;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousAnimationNanos = 0L;
        HandChamsRenderer.reset();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(HandChamsFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.color1);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.color2);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.lineWidth);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.outlineOpacity);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.fillOpacity);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.glowRadius);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.glowIntensity);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.speed);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, HandChamsFeature.gradientScale);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(HandChamsFeature feature, dev.echo.client.setting.FloatSetting preparedLineWidth, dev.echo.client.setting.FloatSetting preparedGlowRadius, dev.echo.client.setting.FloatSetting preparedOutlineOpacity, dev.echo.client.setting.FloatSetting preparedFillOpacity, dev.echo.client.setting.ColorSetting preparedColor2, dev.echo.client.setting.ColorSetting preparedColor1, dev.echo.client.setting.FloatSetting preparedGradientScale, dev.echo.client.setting.FloatSetting preparedGlowIntensity, dev.echo.client.setting.FloatSetting preparedSpeed) {
        HandChamsFeature.lineWidth = preparedLineWidth;
        HandChamsFeature.glowRadius = preparedGlowRadius;
        HandChamsFeature.outlineOpacity = preparedOutlineOpacity;
        HandChamsFeature.fillOpacity = preparedFillOpacity;
        HandChamsFeature.color2 = preparedColor2;
        HandChamsFeature.color1 = preparedColor1;
        HandChamsFeature.gradientScale = preparedGradientScale;
        HandChamsFeature.glowIntensity = preparedGlowIntensity;
        HandChamsFeature.speed = preparedSpeed;
    }
}
