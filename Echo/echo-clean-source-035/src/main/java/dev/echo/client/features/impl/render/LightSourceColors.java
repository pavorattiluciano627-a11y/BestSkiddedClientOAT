package dev.echo.client.features.impl.render;

import dev.echo.client.setting.ColorSetting;

/** Light ESP's luminance-dependent tint and alpha calculation. */
final class LightSourceColors {
    private LightSourceColors() {
    }

    static float[] rgba(int lightLevel, ColorSetting color) {
        float brightness = Math.min(1.0f, Math.max(0.0f, lightLevel / 15.0f));
        float alpha = 0.25f * (1.0f - brightness) + 0.7f * brightness;
        float baseRed = color.getRedFraction();
        float baseGreen = color.getGreenFraction();
        float baseBlue = color.getBlueFraction();
        float red;
        float green;
        float blue;
        if (lightLevel <= 5) {
            float progress = lightLevel / 5.0f;
            red = progress * 0.4f;
            green = progress * 0.4f;
            blue = progress * 0.4f;
        } else if (lightLevel <= 10) {
            float progress = (lightLevel - 5) / 5.0f;
            red = 0.7f + progress * 0.3f;
            green = progress * 0.6f;
            blue = progress * 0.1f;
        } else if (lightLevel <= 14) {
            float progress = (lightLevel - 10) / 4.0f;
            red = 1.0f;
            green = 0.7f + progress * 0.25f;
            blue = 0.2f + progress * 0.3f;
        } else {
            red = 1.0f;
            green = 1.0f;
            blue = 1.0f;
        }
        float blend = brightness * 0.7f;
        return new float[]{
                baseRed * (1.0f - blend) + red * blend,
                baseGreen * (1.0f - blend) + green * blend,
                baseBlue * (1.0f - blend) + blue * blend,
                alpha
        };
    }

    static int argb(int lightLevel, ColorSetting color) {
        float[] rgba = rgba(lightLevel, color);
        return (int) (rgba[3] * 255.0f) << 24
                | (int) (rgba[0] * 255.0f) << 16
                | (int) (rgba[1] * 255.0f) << 8
                | (int) (rgba[2] * 255.0f);
    }
}
