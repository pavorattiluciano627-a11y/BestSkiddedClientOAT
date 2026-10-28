package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;

import static dev.echo.client.features.FeatureLiterals.literal;

/** Settings and color uniforms for the procedural sky renderer. */
public class SkyShadersFeature extends Feature {
    private static final CharSequence AURORA_LABEL = literal("Aurora");
    private static final CharSequence TWILIGHT_LABEL = literal("Twilight");
    public static final int NEBULA = 0;
    public static final int AURORA = 1;
    public static final int TWILIGHT = 2;
    private static ChoiceSetting mode;
    private static BooleanSetting customColors;
    private static ColorSetting primaryColor;
    private static ColorSetting secondaryColor;
    private static FloatSetting speed;
    private static FloatSetting brightness;
    private static FloatSetting stars;
    private static BooleanSetting sunAndMoon;
    private static BooleanSetting endDimension;
    private static String customColorsTooltip;
    private static String colorTooltip;
    private static String speedTooltip;
    private static final float[][] PRIMARY_PALETTES = {
            {0.27f, 0.75f, 0.59f}, {0.35f, 1.0f, 0.62f}, {0.24f, 0.2f, 0.58f}
    };
    private static final float[][] SECONDARY_PALETTES = {
            {0.14f, 0.22f, 0.43f}, {0.55f, 0.3f, 1.0f}, {0.62f, 0.48f, 0.76f}
    };

    public SkyShadersFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Sky Shaders"), dev.echo.client.bootstrap.CatalogText.literal("Replaces the sky with a procedural shader"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        primaryColor.setVisibilityPredicate(ignored -> customColors.getValue());
        secondaryColor.setVisibilityPredicate(ignored -> customColors.getValue());
        // The original uses two distinct cached String objects with equal text.
        if (customColorsTooltip == null) customColorsTooltip = new String("Appearance");
        customColors.tooltip(customColorsTooltip);
        if (colorTooltip == null) colorTooltip = new String("Appearance");
        primaryColor.tooltip(colorTooltip);
        secondaryColor.tooltip(colorTooltip);
        if (speedTooltip == null) speedTooltip = new String("Timing");
        speed.tooltip(speedTooltip);
    }

    public static int getShaderMode() {
        if (mode.isSelected(AURORA_LABEL)) return AURORA;
        if (mode.isSelected(TWILIGHT_LABEL)) return TWILIGHT;
        return NEBULA;
    }

    public static float getPrimaryChannel(int channel) {
        if (customColors.getValue()) return getColorChannel(primaryColor, channel);
        return PRIMARY_PALETTES[getShaderMode()][channel];
    }

    public static float getSecondaryChannel(int channel) {
        if (customColors.getValue()) return getColorChannel(secondaryColor, channel);
        return SECONDARY_PALETTES[getShaderMode()][channel];
    }

    private static float getColorChannel(ColorSetting color, int channel) {
        int value = switch (channel) {
            case 0 -> color.getRed();
            case 1 -> color.getGreen();
            default -> color.getBlue();
        };
        return value / 255.0f;
    }

    public static float getSpeed() { return speed.getValue(); }
    public static float getBrightness() { return brightness.getValue(); }
    public static float getStars() { return stars.getValue(); }
    public static boolean shouldRenderSunAndMoon() { return sunAndMoon.getValue(); }
    public static boolean shouldRenderInEnd() { return endDimension.getValue(); }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SkyShadersFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.customColors);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.primaryColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.secondaryColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.speed);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.brightness);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.stars);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.sunAndMoon);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SkyShadersFeature.endDimension);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SkyShadersFeature feature, dev.echo.client.setting.FloatSetting preparedStars, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.ColorSetting preparedSecondaryColor, dev.echo.client.setting.BooleanSetting preparedEndDimension, dev.echo.client.setting.FloatSetting preparedBrightness, dev.echo.client.setting.BooleanSetting preparedCustomColors, dev.echo.client.setting.BooleanSetting preparedSunAndMoon, dev.echo.client.setting.FloatSetting preparedSpeed, dev.echo.client.setting.ColorSetting preparedPrimaryColor) {
        SkyShadersFeature.stars = preparedStars;
        SkyShadersFeature.mode = preparedMode;
        SkyShadersFeature.secondaryColor = preparedSecondaryColor;
        SkyShadersFeature.endDimension = preparedEndDimension;
        SkyShadersFeature.brightness = preparedBrightness;
        SkyShadersFeature.customColors = preparedCustomColors;
        SkyShadersFeature.sunAndMoon = preparedSunAndMoon;
        SkyShadersFeature.speed = preparedSpeed;
        SkyShadersFeature.primaryColor = preparedPrimaryColor;
    }
}
