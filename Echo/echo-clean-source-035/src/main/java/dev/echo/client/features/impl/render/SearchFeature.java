package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.RegistryListSetting;
import dev.echo.client.setting.specialized.SearchBlockListSetting;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Registers selected block colors with the shared chunk ESP service. */
public class SearchFeature extends Feature {
    static RegistryListSetting<Block> blocks = new SearchBlockListSetting(CatalogText.literal("Blocks"), () -> Registries.BLOCK);
    static FloatSetting opacity;
    static BooleanSetting outline;
    static BooleanSetting tracers;
    private static String opacitySection;
    private static String renderFlagsSection;

    public SearchFeature() {
        super(new FeatureInfo(CatalogText.literal("Search"), CatalogText.literal("Renders selected blocks"),
            Category.RENDER));
    }

    @Override
    public void postInitialize() {
        blocks.onChange(this::updateBlocks);
        opacity.onChange(() -> Echo.blockEspManager.updateAlpha(this, Math.round(opacity.getValue() * 255.0f)));
        outline.onChange(() -> Echo.blockEspManager.setOutline(this, outline.getValue()));
        tracers.onChange(() -> Echo.blockEspManager.setTracers(this, tracers.getValue()));
        if (opacitySection == null) opacitySection = new String("Appearance");
        opacity.tooltip(opacitySection);
        if (renderFlagsSection == null) renderFlagsSection = new String("Appearance");
        outline.tooltip(renderFlagsSection);
        tracers.tooltip(renderFlagsSection);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateBlocks();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.unregister(this);
    }

    private void updateBlocks() {
        if (!isEnabled()) {
            return;
        }
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        int alpha = Math.round(opacity.getValue() * 255.0f);
        for (Identifier id : blocks.getSelectedIds()) {
            Registries.ITEM.getEntry(id).ifPresent(entry -> {
                Block block = Block.getBlockFromItem(entry.value());
                if (block == Blocks.AIR) {
                    return;
                }
                int color = block.getDefaultMapColor().color;
                colors.put(block, alpha << 24 | color & 0x00ffffff);
            });
        }
        Echo.blockEspManager.register(this, colors, outline.getValue(), tracers.getValue());
    }

    public static void appendBootstrapSettings(SearchFeature feature) {
        CatalogSettings.append(feature, blocks);
        CatalogSettings.append(feature, opacity);
        CatalogSettings.append(feature, outline);
        CatalogSettings.append(feature, tracers);
    }

    public static void assignBootstrapSettings(SearchFeature feature, BooleanSetting preparedTracers,
            BooleanSetting preparedOutline, FloatSetting preparedOpacity) {
        tracers = preparedTracers;
        outline = preparedOutline;
        opacity = preparedOpacity;
    }
}
