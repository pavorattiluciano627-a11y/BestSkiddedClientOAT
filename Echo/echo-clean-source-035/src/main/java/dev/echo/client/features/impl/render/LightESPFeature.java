package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.IntSetting;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

/** Luminance-based block highlights; linkage and branch domains remain conditional. */
public class LightESPFeature extends Feature {
    private static IntSetting maxY;
    private static IntSetting minLight;
    private static ColorSetting color;
    private static BooleanSetting outline;
    private static BooleanSetting tracers;
    private static String colorTooltip;
    private static String renderTooltip;

    public LightESPFeature() {
        super(new FeatureInfo(CatalogText.literal("Light ESP"),
                CatalogText.literal("Scans for out of place light sources"), Category.RENDER, false));
    }

    @Override
    public void postInitialize() {
        maxY.onChange(this::rebuildLightSources);
        minLight.onChange(this::rebuildLightSources);
        color.onChange(this::rebuildLightSources);
        outline.onChange(() -> Echo.blockEspManager.setOutline(this, outline.getValue()));
        tracers.onChange(() -> Echo.blockEspManager.setTracers(this, tracers.getValue()));
        if (colorTooltip == null) colorTooltip = new String("Appearance");
        color.tooltip(colorTooltip);
        if (renderTooltip == null) renderTooltip = new String("Appearance");
        outline.tooltip(renderTooltip);
        tracers.tooltip(renderTooltip);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rebuildLightSources();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.unregister(this);
    }

    private void rebuildLightSources() {
        if (isOutsideWorld() || !isEnabled()) return;
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        int minimum = minLight.getValue();
        for (Block block : Registries.BLOCK) {
            int luminance = block.getDefaultState().getLuminance();
            if (luminance >= minimum) colors.put(block, packedColorForLightLevel(luminance));
        }
        // The original rebuild reads minLight, not maxY. Preserve its actual arguments.
        Echo.blockEspManager.register(this, colors, outline.getValue(), tracers.getValue());
    }

    private float[] colorForLightLevel(int lightLevel) {
        return LightSourceColors.rgba(lightLevel, color);
    }

    private int packedColorForLightLevel(int lightLevel) {
        float[] rgba = colorForLightLevel(lightLevel);
        return (int) (rgba[3] * 255.0f) << 24
                | (int) (rgba[0] * 255.0f) << 16
                | (int) (rgba[1] * 255.0f) << 8
                | (int) (rgba[2] * 255.0f);
    }

    /** Appends settings in original declaring-class order. */
    public static void appendBootstrapSettings(LightESPFeature feature) {
        CatalogSettings.append(feature, maxY);
        CatalogSettings.append(feature, minLight);
        CatalogSettings.append(feature, color);
        CatalogSettings.append(feature, outline);
        CatalogSettings.append(feature, tracers);
    }

    /** Publication follows successful construction of the complete metadata vector. */
    public static void assignBootstrapSettings(LightESPFeature feature, IntSetting preparedMaxY,
            IntSetting preparedMinLight, BooleanSetting preparedTracers,
            BooleanSetting preparedOutline, ColorSetting preparedColor) {
        maxY = preparedMaxY;
        minLight = preparedMinLight;
        tracers = preparedTracers;
        outline = preparedOutline;
        color = preparedColor;
    }
}
