package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.features.Feature;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.utils.blocks.ContainerBlocks;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Registers per-block storage colors with the shared asynchronous block ESP manager. */
public class StorageESPFeature extends Feature {
    private static ColorSetting chestColor;
    private static ColorSetting barrelColor;
    private static ColorSetting shulkerColor;
    private static ColorSetting enderChestColor;
    private static ColorSetting otherStorageColor;
    private static BooleanSetting outline;
    private static BooleanSetting tracers;

    public StorageESPFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Storage ESP"), dev.echo.client.bootstrap.CatalogText.literal("Highlights storage blocks"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        chestColor.onChange(this::rebuildRegistration);
        barrelColor.onChange(this::rebuildRegistration);
        shulkerColor.onChange(this::rebuildRegistration);
        enderChestColor.onChange(this::rebuildRegistration);
        otherStorageColor.onChange(this::rebuildRegistration);
        outline.onChange(() -> Echo.blockEspManager.setOutline(this, outline.getValue()));
        tracers.onChange(() -> Echo.blockEspManager.setTracers(this, tracers.getValue()));
        chestColor.tooltip("Appearance");
        barrelColor.tooltip("Appearance");
        shulkerColor.tooltip("Appearance");
        enderChestColor.tooltip("Appearance");
        otherStorageColor.tooltip("Appearance");
        outline.tooltip("Appearance");
        tracers.tooltip("Appearance");
    }

    private void rebuildRegistration() {
        boolean bypass = FreecamFeature.isPerspectiveRefreshBypassed();
        if (!isEnabled()) {
            return;
        }
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (bypass) {
                return;
            }
            if (ContainerBlocks.isContainer(block)) {
                colors.put(block, colorForBlock(block));
            }
        }
        Echo.blockEspManager.register(this, colors, outline.getValue(), tracers.getValue());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rebuildRegistration();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Echo.blockEspManager.unregister(this);
    }

    private int colorForBlock(Block block) {
        // All five values are read before classification, even for a known block type.
        int chest = chestColor.getArgb();
        int barrel = barrelColor.getArgb();
        boolean selectByType = FreecamFeature.isPerspectiveRefreshEnabled();
        int shulker = shulkerColor.getArgb();
        int enderChest = enderChestColor.getArgb();
        int other = otherStorageColor.getArgb();
        if (!selectByType) {
            EnderChestBlock originalCast = (EnderChestBlock) block;
            return enderChest;
        }
        if (block instanceof EnderChestBlock) {
            return enderChest;
        }
        if (block instanceof AbstractChestBlock<?>) {
            return chest;
        }
        if (block instanceof BarrelBlock) {
            return barrel;
        }
        if (block instanceof ShulkerBoxBlock) {
            return shulker;
        }
        return other;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(StorageESPFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.chestColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.barrelColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.shulkerColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.enderChestColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.otherStorageColor);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.outline);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StorageESPFeature.tracers);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(StorageESPFeature feature, dev.echo.client.setting.ColorSetting preparedOtherStorageColor, dev.echo.client.setting.ColorSetting preparedShulkerColor, dev.echo.client.setting.ColorSetting preparedBarrelColor, dev.echo.client.setting.BooleanSetting preparedOutline, dev.echo.client.setting.ColorSetting preparedEnderChestColor, dev.echo.client.setting.BooleanSetting preparedTracers, dev.echo.client.setting.ColorSetting preparedChestColor) {
        StorageESPFeature.otherStorageColor = preparedOtherStorageColor;
        StorageESPFeature.shulkerColor = preparedShulkerColor;
        StorageESPFeature.barrelColor = preparedBarrelColor;
        StorageESPFeature.outline = preparedOutline;
        StorageESPFeature.enderChestColor = preparedEnderChestColor;
        StorageESPFeature.tracers = preparedTracers;
        StorageESPFeature.chestColor = preparedChestColor;
    }
}
